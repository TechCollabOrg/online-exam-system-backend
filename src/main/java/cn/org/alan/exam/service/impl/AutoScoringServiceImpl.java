package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.config.AiGradingProperties;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.common.enums.AiFeatureCode;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.question.QuestionScoreVO;
import cn.org.alan.exam.service.IAiGradingWebSearchService;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import cn.org.alan.exam.service.IAutoScoringService;
import cn.org.alan.exam.utils.AiGradingQuestionLoader;
import cn.org.alan.exam.utils.AiGradingResponseParser;
import cn.org.alan.exam.utils.AiGradingTextUtil;
import cn.org.alan.exam.utils.agent.AIChat;
import cn.org.alan.exam.utils.agent.Constants;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 主观题 AI 辅助评分：与教师阅卷列表一致加载全部待评题，逐题调用模型并独立提交，避免一题失败导致全部回滚。
 */
@Slf4j
@Service
public class AutoScoringServiceImpl extends ServiceImpl<ExamQuAnswerMapper, ExamQuAnswer> implements IAutoScoringService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000L;

    @Autowired
    private AiGradingQuestionLoader aiGradingQuestionLoader;

    @Autowired
    private AIChat aiChat;

    @Autowired
    private IAiGradingWebSearchService aiGradingWebSearchService;

    @Autowired
    private AiGradingProperties aiGradingProperties;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @Override
    @Async
    public void autoScoringExam(Integer examId, Integer userId) {
        try {
            doAutoScoringExam(examId, userId, false);
        } catch (Exception e) {
            log.error("异步 AI 阅卷失败 examId={} userId={}", examId, userId, e);
        }
    }

    @Override
    public int autoScoringExamSync(Integer examId, Integer userId) {
        assertAiConfigured();
        return doAutoScoringExam(examId, userId, true);
    }

    private void assertAiConfigured() {
        LlmResolvedConfig active = aiPlatformConfigService.resolveForFeature(AiFeatureCode.GRADING);
        if (active == null || StringUtils.isBlank(active.getApiKey())) {
            throw new ServiceRuntimeException("请由管理员在「API 连接配置」中为 AI 阅卷保存并启用接口");
        }
    }

    private int doAutoScoringExam(Integer examId, Integer userId, boolean throwOnFailure) {
        List<QuestionScoreVO> questions = aiGradingQuestionLoader.load(examId, userId);
        if (questions == null || questions.isEmpty()) {
            log.info("AI阅卷跳过：无待评主观题 examId={} userId={}", examId, userId);
            return 0;
        }

        int total = questions.size();
        int graded = 0;
        List<Integer> failedIds = new ArrayList<>();

        for (QuestionScoreVO q : questions) {
            Integer qid = parseQuestionIdFromVo(q);
            try {
                gradeOneQuestionWithRetry(examId, userId, q, qid);
                graded++;
            } catch (Exception e) {
                failedIds.add(qid);
                log.warn("AI阅卷单题失败 examId={} userId={} questionId={}: {}", examId, userId, qid, e.getMessage());
            }
        }

        log.info("AI阅卷结束 examId={} userId={} 成功={}/{}", examId, userId, graded, total);

        if (throwOnFailure && graded == 0) {
            throw new ServiceRuntimeException("AI 阅卷失败：共 " + total + " 题均未评分成功，请检查 API 配置或稍后重试");
        }
        if (throwOnFailure && !failedIds.isEmpty()) {
            throw new ServiceRuntimeException(
                    "AI 阅卷部分完成：成功 " + graded + "/" + total + " 题，未成功题目ID：" + failedIds);
        }
        return graded;
    }

    private void gradeOneQuestionWithRetry(Integer examId, Integer userId, QuestionScoreVO q, Integer qid) throws Exception {
        Exception last = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = platformTransactionManager.getTransaction(def);
            try {
                prepareQuestionForModel(q);
                if (AiGradingTextUtil.isBlankAnswer(q.getUserAnswer())) {
                    persistAiResult(examId, userId, qid, 0,
                            AiGradingTextUtil.formatAiReason("未作答，0分"));
                } else {
                    gradeSingleQuestion(examId, userId, q, qid);
                }
                platformTransactionManager.commit(status);
                return;
            } catch (Exception e) {
                platformTransactionManager.rollback(status);
                last = e;
                if (attempt < MAX_ATTEMPTS) {
                    try {
                        TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new ServiceRuntimeException("AI 阅卷被中断");
                    }
                }
            }
        }
        if (last != null) {
            throw last;
        }
    }

    private void gradeSingleQuestion(Integer examId, Integer userId, QuestionScoreVO question, Integer questionId)
            throws Exception {
        String payload = JSONUtil.toJsonStr(Collections.singletonList(question));
        String response = aiChat.getGradingResponse(Constants.systemMessage, payload).trim();
        log.debug("AI阅卷单题回复 questionId={} length={}", questionId, response.length());

        JSONArray scoreArray = AiGradingResponseParser.parseScoreItems(response);
        if (scoreArray == null || scoreArray.isEmpty()) {
            throw new ServiceRuntimeException("AI 返回无法解析为评分结果 JSON，questionId=" + questionId);
        }

        JSONObject item = findScoreItem(scoreArray, questionId);
        int rawScore = AiGradingResponseParser.parseFinalScore(item);
        int finalScore = AiGradingTextUtil.clampScore(rawScore, question.getTotalScore());
        String reason = AiGradingTextUtil.formatAiReason(item.getStr("扣分原因"));
        if (rawScore != finalScore) {
            reason = AiGradingTextUtil.formatAiReason(
                    item.getStr("扣分原因") + "（已按满分" + question.getTotalScore() + "分钳制）");
        }

        persistAiResult(examId, userId, questionId, finalScore, reason);
    }

    /**
     * 从模型返回的评分数组中按题目 ID 匹配；单题请求时若 ID 不一致则仍采用该条结果并记日志。
     */
    private JSONObject findScoreItem(JSONArray scoreArray, Integer questionId) {
        for (int i = 0; i < scoreArray.size(); i++) {
            JSONObject item = scoreArray.getJSONObject(i);
            try {
                if (questionId.equals(AiGradingResponseParser.parseQuestionId(item))) {
                    return item;
                }
            } catch (Exception ignored) {
                // 跳过无法解析 ID 的项
            }
        }
        JSONObject fallback = scoreArray.getJSONObject(0);
        try {
            Integer returnedId = AiGradingResponseParser.parseQuestionId(fallback);
            if (!questionId.equals(returnedId)) {
                log.warn("AI 返回题目ID与请求不一致，仍采用首条评分结果 期望={} 实际={}", questionId, returnedId);
            }
        } catch (Exception e) {
            log.warn("AI 返回缺少题目ID，采用首条评分结果 questionId={}", questionId);
        }
        return fallback;
    }

    private void prepareQuestionForModel(QuestionScoreVO q) {
        q.setQuestionContent(AiGradingTextUtil.stripHtml(q.getQuestionContent()));
        q.setQusetionAnswer(AiGradingTextUtil.stripHtml(q.getQusetionAnswer()));
        q.setQuestionAnalysis(AiGradingTextUtil.stripHtml(q.getQuestionAnalysis()));
        q.setUserAnswer(AiGradingTextUtil.stripHtml(q.getUserAnswer()));

        if (aiGradingProperties.isWebSearchEnabled() && StringUtils.isBlank(q.getReferenceMaterial())) {
            String ref = aiGradingWebSearchService.searchReference(q.getQuestionContent());
            q.setReferenceMaterial(StringUtils.isNotBlank(ref) ? ref : "");
        } else if (StringUtils.isBlank(q.getReferenceMaterial())) {
            q.setReferenceMaterial("");
        }
        if (StringUtils.isBlank(q.getQuestionAnalysis())) {
            q.setQuestionAnalysis("");
        }
    }

    private void persistAiResult(Integer examId, Integer userId, Integer questionId, int score, String reason) {
        LambdaQueryWrapper<ExamQuAnswer> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ExamQuAnswer::getExamId, examId)
                .eq(ExamQuAnswer::getUserId, userId)
                .eq(ExamQuAnswer::getQuestionId, questionId);

        ExamQuAnswer existing = getOne(queryWrapper);
        if (existing == null) {
            throw new ServiceRuntimeException("未找到作答记录 questionId=" + questionId);
        }

        ExamQuAnswer update = new ExamQuAnswer();
        update.setId(existing.getId());
        update.setAiScore(score);
        update.setAiReason(reason);
        updateById(update);
    }

    private Integer parseQuestionIdFromVo(QuestionScoreVO q) {
        if (q == null || StringUtils.isBlank(q.getQuestionId())) {
            throw new ServiceRuntimeException("题目缺少题目ID");
        }
        return Integer.valueOf(q.getQuestionId().trim());
    }
}
