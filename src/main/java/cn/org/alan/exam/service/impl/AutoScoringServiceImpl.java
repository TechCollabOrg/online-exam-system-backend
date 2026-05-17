package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.vo.question.QuestionScoreVO;
import cn.org.alan.exam.service.IAiGradingWebSearchService;
import cn.org.alan.exam.service.IAutoScoringService;
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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 主观题 AI 辅助评分：逐题调用模型、钳制分数、写回 aiScore / aiReason（带【AI阅卷】标记）。
 */
@Slf4j
@Service
public class AutoScoringServiceImpl extends ServiceImpl<ExamQuAnswerMapper, ExamQuAnswer> implements IAutoScoringService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 5000L;

    @Autowired
    private ExamQuAnswerMapper examQuAnswerMapper;

    @Autowired
    private AIChat aiChat;

    @Autowired
    private IAiGradingWebSearchService aiGradingWebSearchService;

    @Autowired
    private PlatformTransactionManager platformTransactionManager;

    @Override
    @Async
    public void autoScoringExam(Integer examId, Integer userId) {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = platformTransactionManager.getTransaction(def);
            try {
                List<QuestionScoreVO> questions = examQuAnswerMapper.getQuestionsForGrading(examId, userId);
                if (questions == null || questions.isEmpty()) {
                    log.info("AI阅卷跳过：无待评主观题 examId={} userId={}", examId, userId);
                    platformTransactionManager.commit(status);
                    return;
                }

                for (QuestionScoreVO q : questions) {
                    prepareQuestionForModel(q);
                    Integer qid = parseQuestionIdFromVo(q);

                    if (AiGradingTextUtil.isBlankAnswer(q.getUserAnswer())) {
                        persistAiResult(examId, userId, qid, 0,
                                AiGradingTextUtil.formatAiReason("未作答，0分"));
                        continue;
                    }

                    gradeSingleQuestion(examId, userId, q, qid);
                }

                platformTransactionManager.commit(status);
                log.info("AI阅卷完成 examId={} userId={} 题数={}", examId, userId, questions.size());
                return;
            } catch (Exception e) {
                platformTransactionManager.rollback(status);
                log.warn("AI阅卷失败 第{}次 examId={} userId={}: {}", attempt, examId, userId, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    log.error("AI阅卷重试耗尽 examId={} userId={}", examId, userId, e);
                    return;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
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

        JSONObject item = scoreArray.getJSONObject(0);
        Integer returnedId = AiGradingResponseParser.parseQuestionId(item);
        if (!questionId.equals(returnedId)) {
            throw new ServiceRuntimeException("AI 返回题目ID不匹配，期望" + questionId + "实际" + returnedId);
        }

        int rawScore = AiGradingResponseParser.parseFinalScore(item);
        int finalScore = AiGradingTextUtil.clampScore(rawScore, question.getTotalScore());
        String reason = AiGradingTextUtil.formatAiReason(item.getStr("扣分原因"));
        if (rawScore != finalScore) {
            reason = AiGradingTextUtil.formatAiReason(
                    item.getStr("扣分原因") + "（已按满分" + question.getTotalScore() + "分钳制）");
        }

        persistAiResult(examId, userId, questionId, finalScore, reason);
    }

    private void prepareQuestionForModel(QuestionScoreVO q) {
        q.setQuestionContent(AiGradingTextUtil.stripHtml(q.getQuestionContent()));
        q.setQusetionAnswer(AiGradingTextUtil.stripHtml(q.getQusetionAnswer()));
        q.setQuestionAnalysis(AiGradingTextUtil.stripHtml(q.getQuestionAnalysis()));
        q.setUserAnswer(AiGradingTextUtil.stripHtml(q.getUserAnswer()));

        if (StringUtils.isBlank(q.getReferenceMaterial())) {
            String ref = aiGradingWebSearchService.searchReference(q.getQuestionContent());
            q.setReferenceMaterial(StringUtils.isNotBlank(ref) ? ref : "");
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
