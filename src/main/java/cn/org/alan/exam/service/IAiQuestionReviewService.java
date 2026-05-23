package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiQuestionReviewForm;
import cn.org.alan.exam.model.vo.ai.AiQuestionReviewVO;

/**
 * 学生考后单题 AI 解析。
 */
public interface IAiQuestionReviewService {

    Result<AiQuestionReviewVO> analyzeQuestion(AiQuestionReviewForm form);
}
