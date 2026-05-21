package cn.org.alan.exam.service.impl;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.mapper.ExamQuAnswerMapper;
import cn.org.alan.exam.mapper.ExamQuestionMapper;
import cn.org.alan.exam.mapper.QuestionMapper;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.entity.ExamQuAnswer;
import cn.org.alan.exam.model.entity.ExamQuestion;
import cn.org.alan.exam.model.entity.Question;
import cn.org.alan.exam.model.vo.question.QuestionScoreVO;
import cn.org.alan.exam.utils.ExamGradingUtil;
import cn.org.alan.exam.service.IAiGradingWebSearchService;
import cn.org.alan.exam.service.IAiPlatformConfigService;
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

import javax.annotation.Resource;
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
    private ExamQuestionMapper examQuestionMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private AIChat aiChat;

    @Autowired
    private IAiGradingWebSearchService aiGradingWebSearchService;

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
        LlmResolvedConfig active = aiPlatformConfigService.resolveActive();
        if (active == null || StringUtils.isBlank(active.getApiKey())) {
            throw new ServiceRuntimeException("请由管理员在「API 连接配置」中保存并启用 AI 接口后再使用 AI 阅卷");
        }
    }

    /**
     * @param throwOnFailure 为 true 时（教师手动触发）在重试耗尽后抛出异常
     * @return 成功写入 AI 分数的题目数
     */
    private int doAutoScoringExam(Integer examId, Integer userId, boolean throwOnFailure) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            DefaultTransactionDefinition def = new DefaultTransactionDefinition();
            def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            TransactionStatus status = platformTransactionManager.getTransaction(def);
            try {
                int graded = runGradingInTransaction(examId, userId);
                platformTransactionManager.commit(status);
                log.info("AI阅卷完成 examId={} userId={} 题数={}", examId, userId, graded);
                return graded;
            } catch (Exception e) {
                platformTransactionManager.rollback(status);
                lastError = e;
                log.warn("AI阅卷失败 第{}次 examId={} userId={}: {}", attempt, examId, userId, e.getMessage());
                if (attempt == MAX_ATTEMPTS) {
                    if (throwOnFailure) {
                        String msg = e.getMessage() != null ? e.getMessage() : "未知错误";
                        throw new ServiceRuntimeException("AI 阅卷失败：" + msg);
                    }
                    return 0;
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    if (throwOnFailure) {
                        throw new ServiceRuntimeException("AI 阅卷被中断");
                    }
                    return 0;
                }
            }
        }
        if (throwOnFailure && lastError != null) {
            throw new ServiceRuntimeException("AI 阅卷失败：" + lastError.getMessage());
        }
        return 0;
    }

    private int runGradingInTransaction(Integer examId, Integer userId) throws Exception {
        List<QuestionScoreVO> questions = new java.util.ArrayList<>();
        List<QuestionScoreVO> saqList = examQuAnswerMapper.getQuestionsForGrading(examId, userId);
        if (saqList != null) {
            questions.addAll(saqList);
        }
        questions.addAll(buildCompoundGradingQuestions(examId, userId));
        if (questions.isEmpty()) {
            log.info("AI阅卷跳过：无待评主观题 examId={} userId={}", examId, userId);
            return 0;
        }

        int graded = 0;
        for (QuestionScoreVO q : questions) {
            prepareQuestionForModel(q);
            Integer qid = parseQuestionIdFromVo(q);

            if (AiGradingTextUtil.isBlankAnswer(q.getUserAnswer())) {
                persistAiResult(examId, userId, qid, 0,
                        AiGradingTextUtil.formatAiReason("未作答，0分"));
                graded++;
                continue;
            }

            gradeSingleQuestion(examId, userId, q, qid);
            graded++;
        }
        return graded;
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

    /**
     * 含简答小问的复合题：组装为 AI 可评的单条记录（共用材料 + 各简答子题作答）。
     */
    private List<QuestionScoreVO> buildCompoundGradingQuestions(Integer examId, Integer userId) {
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuestion> eqQw =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        eqQw.eq(ExamQuestion::getExamId, examId).eq(ExamQuestion::getType, 5);
        List<ExamQuestion> examQuestions = examQuestionMapper.selectList(eqQw);
        if (examQuestions == null || examQuestions.isEmpty()) {
            return Collections.emptyList();
        }
        List<QuestionScoreVO> list = new java.util.ArrayList<>();
        for (ExamQuestion eq : examQuestions) {
            if (eq.getQuestionId() == null) {
                continue;
            }
            Question question = questionMapper.selectById(eq.getQuestionId());
            if (!ExamGradingUtil.compoundNeedsManualGrading(question)) {
                continue;
            }
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ExamQuAnswer> ansQw =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            ansQw.eq(ExamQuAnswer::getExamId, examId)
                    .eq(ExamQuAnswer::getUserId, userId)
                    .eq(ExamQuAnswer::getQuestionId, eq.getQuestionId())
                    .last("limit 1");
            ExamQuAnswer answer = examQuAnswerMapper.selectOne(ansQw);
            if (answer == null) {
                continue;
            }
            QuestionScoreVO vo = new QuestionScoreVO();
            vo.setQuestionId(String.valueOf(eq.getQuestionId()));
            String refBlock = ExamGradingUtil.formatCompoundSaqReference(question);
            vo.setQuestionContent(AiGradingTextUtil.stripHtml(question.getContent())
                    + (StringUtils.isNotBlank(refBlock) ? "\n【简答子题参考答案】\n" + refBlock : ""));
            vo.setQusetionAnswer(ExamGradingUtil.formatCompoundSaqReference(question));
            vo.setQuestionAnalysis(AiGradingTextUtil.stripHtml(question.getAnalysis()));
            vo.setUserAnswer(ExamGradingUtil.formatCompoundSaqStudentAnswer(question, answer.getAnswerContent()));
            vo.setTotalScore(eq.getScore() != null ? eq.getScore() : 0);
            list.add(vo);
        }
        return list;
    }
}
