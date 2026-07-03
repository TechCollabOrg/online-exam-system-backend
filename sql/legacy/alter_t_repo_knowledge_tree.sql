-- 题库知识树：AI 根据题目生成的知识点层级结构（JSON）
ALTER TABLE `t_repo`
    ADD COLUMN `knowledge_tree` mediumtext COLLATE utf8mb4_bin DEFAULT NULL COMMENT '知识树 JSON' AFTER `is_exercise`,
    ADD COLUMN `knowledge_tree_time` datetime DEFAULT NULL COMMENT '知识树最近生成时间' AFTER `knowledge_tree`;
