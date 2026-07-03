-- 材料题 / 共用题干：小题行通过 parent_qu_id 指向仅存放共用题干（文字与图）的题目 id。
ALTER TABLE `t_question`
  ADD COLUMN `parent_qu_id` int DEFAULT NULL COMMENT '共用题干所属题目 id（t_question.id）；非空表示本题为该题干下的小题' AFTER `is_deleted`;
