-- 旧库分值升级为「展示分 × 100」库存格式（仅执行一次）
-- 判断：总分 gross_score 在 (0, 1000) 视为未乘 100 的旧数据；已升级库请勿重复执行。
-- 若某表无数据或列不存在，对应语句可跳过。

UPDATE t_exam
SET passed_score   = passed_score * 100,
    gross_score    = gross_score * 100,
    radio_score    = IF(radio_score IS NULL, NULL, radio_score * 100),
    multi_score    = IF(multi_score IS NULL, NULL, multi_score * 100),
    judge_score    = IF(judge_score IS NULL, NULL, judge_score * 100),
    saq_score      = IF(saq_score IS NULL, NULL, saq_score * 100),
    compound_score = compound_score * 100
WHERE gross_score > 0 AND gross_score < 1000;

UPDATE t_exam_question
SET score = score * 100
WHERE score > 0 AND score < 1000;

UPDATE t_exam_qu_answer
SET score = score * 100
WHERE score IS NOT NULL AND score > 0 AND score < 1000;

UPDATE t_exam_qu_answer
SET ai_score = ai_score * 100
WHERE ai_score IS NOT NULL AND ai_score > 0 AND ai_score < 1000;

UPDATE t_manual_score
SET score = score * 100
WHERE score IS NOT NULL AND score > 0 AND score < 1000;

UPDATE t_user_exams_score
SET user_score = user_score * 100
WHERE user_score IS NOT NULL AND user_score > 0 AND user_score < 1000;
