-- 组卷：复合题单独题型统计与默认分值（执行一次）
ALTER TABLE t_exam
    ADD COLUMN compound_count INT NOT NULL DEFAULT 0 COMMENT '复合题数量' AFTER saq_score,
    ADD COLUMN compound_score INT NOT NULL DEFAULT 0 COMMENT '复合题单题分值' AFTER compound_count;
