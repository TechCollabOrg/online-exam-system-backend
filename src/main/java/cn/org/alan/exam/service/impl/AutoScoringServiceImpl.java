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
import cn.org.alan.exam.utils.agent.AIChat;
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

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 主观题 AI 辅助评分：结合题目解析与可选联网检索，调用 {@link AIChat} 后写回 aiScore / aiReason。
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

                enrichGradingContext(questions);

                String scoringRequest = JSONUtil.toJsonStr(questions);
                String response = aiChat.getChatResponse(scoringRequest).trim();
                log.debug("AI阅卷原始回复 examId={} userId={} length={}", examId, userId, response.length());

                JSONArray scoreArray = AiGradingResponseParser.parseScoreItems(response);
                if (scoreArray == null || scoreArray.isEmpty()) {
                    throw new ServiceRuntimeException("AI 返回无法解析为评分结果 JSON");
                }

                applyScoreResults(examId, userId, scoreArray);
                platformTransactionManager.commit(status);
                log.info("AI阅卷完成 examId={} userId={} 题数={}", examId, userId, scoreArray.size());
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

    private void enrichGradingContext(List<QuestionScoreVO> questions) {
        for (QuestionScoreVO q : questions) {
            if (StringUtils.isBlank(q.getReferenceMaterial())) {
                String plain = stripHtml(q.getQuestionContent());
                String ref = aiGradingWebSearchService.searchReference(plain);
                if (StringUtils.isNotBlank(ref)) {
                    q.setReferenceMaterial(ref);
                }
            }
            if (StringUtils.isBlank(q.getQuestionAnalysis())) {
                q.setQuestionAnalysis("");
            }
            if (StringUtils.isBlank(q.getReferenceMaterial())) {
                q.setReferenceMaterial("");
            }
        }
    }

    private void applyScoreResults(Integer examId, Integer userId, JSONArray scoreArray) {
        for (int i = 0; i < scoreArray.size(); i++) {
            JSONObject item = scoreArray.getJSONObject(i);
            Integer questionId = parseQuestionId(item);
            int finalScore = AiGradingResponseParser.parseFinalScore(item);
            String reason = item.getStr("扣分原因");
            if (reason == null) {
                reason = "";
            }

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
            update.setAiScore(finalScore);
            update.setAiReason(reason);
            updateById(update);
        }
    }

    private Integer parseQuestionId(JSONObject item) {
        Object raw = item.get("题目ID");
        if (raw == null) {
            throw new ServiceRuntimeException("评分结果缺少题目ID");
        }
        if (raw instanceof Number) {
            return ((Number) raw).intValue();
        }
        return Integer.valueOf(String.valueOf(raw).trim());
    }

    private static String stripHtml(String html) {
        if (StringUtils.isBlank(html)) {
            return "";
        }
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
