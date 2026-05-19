-- 管理员端 AI API 连接配置（单例 id=1）
CREATE TABLE IF NOT EXISTS `t_ai_platform_config` (
  `id` int(11) NOT NULL DEFAULT 1 COMMENT '固定为 1',
  `base_url` varchar(512) NOT NULL DEFAULT '' COMMENT 'OpenAI 兼容基础 URL，如 https://api.siliconflow.cn/v1',
  `api_key` varchar(512) NOT NULL DEFAULT '' COMMENT 'API 密钥',
  `model_name` varchar(128) NOT NULL DEFAULT '' COMMENT '模型 ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '1 启用 0 禁用',
  `last_test_ok` tinyint(1) DEFAULT NULL COMMENT '上次连接测试是否成功',
  `last_test_time` datetime DEFAULT NULL COMMENT '上次连接测试时间',
  `update_user_id` int(11) DEFAULT NULL COMMENT '最后修改人',
  `update_time` datetime DEFAULT NULL COMMENT '最后修改时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 平台连接配置（管理员维护）';

INSERT INTO `t_ai_platform_config` (`id`, `base_url`, `api_key`, `model_name`, `enabled`)
SELECT 1, '', '', '', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_platform_config` WHERE `id` = 1);
