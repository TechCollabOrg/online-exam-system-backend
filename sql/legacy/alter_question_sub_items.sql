-- 复合题：在 t_question 增加 sub_items 字段，用于存储多小题 JSON
ALTER TABLE `t_question`
  ADD COLUMN `sub_items` mediumtext NULL COMMENT '复合题（题型5）小题 JSON' AFTER `content`;
