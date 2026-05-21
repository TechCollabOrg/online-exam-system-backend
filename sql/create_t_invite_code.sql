-- 注册邀请码表：教师/管理员注册须凭码；仅管理员可生成与管理
CREATE TABLE IF NOT EXISTS `t_invite_code` (
  `id` int NOT NULL AUTO_INCREMENT,
  `code` varchar(32) NOT NULL COMMENT '邀请码',
  `role_id` int NOT NULL COMMENT '2教师 3管理员',
  `max_uses` int NOT NULL DEFAULT 1 COMMENT '最大可用次数',
  `used_count` int NOT NULL DEFAULT 0 COMMENT '已使用次数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1启用 0禁用',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `expire_time` datetime DEFAULT NULL COMMENT '过期时间，NULL表示不过期',
  `creator_id` int DEFAULT NULL COMMENT '创建人（管理员）',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invite_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='注册邀请码';
