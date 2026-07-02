-- 按学生发布考试：目标类型 + 考试-学生关联表
-- target_type: 1=按班级（默认），2=按指定学生

ALTER TABLE `t_exam`
    ADD COLUMN `target_type` tinyint NOT NULL DEFAULT 1 COMMENT '发布范围：1按班级 2按学生' AFTER `is_deleted`;

DROP TABLE IF EXISTS `t_exam_user`;
CREATE TABLE `t_exam_user` (
  `id` int NOT NULL AUTO_INCREMENT COMMENT '考试与学生关系表ID',
  `exam_id` int NOT NULL COMMENT '考试ID',
  `user_id` int NOT NULL COMMENT '学生用户ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_exam_user` (`exam_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_bin COMMENT='考试与指定学生关联';
