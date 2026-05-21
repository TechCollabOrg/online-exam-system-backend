-- 作答表：单题得分（客观题自动写入或教师批改后写入；AI 建议分仍用 ai_score）
-- 若列已存在会报 Duplicate column，可跳过。
ALTER TABLE t_exam_qu_answer
    ADD COLUMN score INT NULL DEFAULT NULL COMMENT '该题得分' AFTER is_right;
