-- 将历史分值统一为「存储 = 展示分 × 100」（与 Exam 实体注释一致）
-- 若某列已是 ×100 后的数值，请勿重复执行；执行前请备份数据库。
-- 新装库若已按 db_exam.sql 初始化且从未以「展示分」直存，执行本脚本一次即可。

UPDATE t_exam_question SET score = score * 100 WHERE score IS NOT NULL AND score < 100000;

UPDATE t_exam SET
    gross_score = gross_score * 100,
    passed_score = passed_score * 100,
    radio_score = IFNULL(radio_score, 0) * 100,
    multi_score = IFNULL(multi_score, 0) * 100,
    judge_score = IFNULL(judge_score, 0) * 100,
    saq_score = IFNULL(saq_score, 0) * 100,
    compound_score = IFNULL(compound_score, 0) * 100
WHERE id > 0;

UPDATE t_exam_qu_answer SET score = score * 100 WHERE score IS NOT NULL AND score < 100000;
UPDATE t_exam_qu_answer SET ai_score = ai_score * 100 WHERE ai_score IS NOT NULL AND ai_score < 100000;

UPDATE t_user_exams_score SET user_score = user_score * 100 WHERE user_score IS NOT NULL AND user_score < 10000000;
