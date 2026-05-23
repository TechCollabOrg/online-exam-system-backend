-- AI 助手 RAG 知识库（管理员维护，替代写死在 classpath/ai-knowledge/*.md）
CREATE TABLE IF NOT EXISTS `t_ai_knowledge_doc` (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` varchar(200) NOT NULL DEFAULT '' COMMENT '文档标题（管理端展示）',
  `content` mediumtext NOT NULL COMMENT 'Markdown 正文',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1 启用参与检索 0 停用',
  `sort_order` int(11) NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
  `create_user_id` int(11) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_user_id` int(11) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 助手知识库文档';
