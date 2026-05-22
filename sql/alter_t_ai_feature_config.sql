-- 各 AI 功能可单独配置 API，未单独配置时沿用 t_ai_platform_config（id=1）默认连接
CREATE TABLE IF NOT EXISTS `t_ai_feature_config` (
  `feature_code` varchar(32) NOT NULL COMMENT '功能编码：grading/assistant/briefing/question_review',
  `use_default` tinyint(1) NOT NULL DEFAULT 1 COMMENT '1=沿用默认连接 0=使用本行配置',
  `base_url` varchar(512) NOT NULL DEFAULT '' COMMENT 'OpenAI 兼容基础 URL',
  `api_key` varchar(512) NOT NULL DEFAULT '' COMMENT 'API 密钥',
  `model_name` varchar(128) NOT NULL DEFAULT '' COMMENT '模型 ID',
  `enabled` tinyint(1) NOT NULL DEFAULT 0 COMMENT '单独配置时是否启用：1 是 0 否',
  `update_user_id` int(11) DEFAULT NULL,
  `update_time` datetime DEFAULT NULL,
  PRIMARY KEY (`feature_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI 分功能 API 配置';

INSERT INTO `t_ai_feature_config` (`feature_code`, `use_default`, `enabled`)
SELECT 'grading', 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_feature_config` WHERE `feature_code` = 'grading');

INSERT INTO `t_ai_feature_config` (`feature_code`, `use_default`, `enabled`)
SELECT 'assistant', 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_feature_config` WHERE `feature_code` = 'assistant');

INSERT INTO `t_ai_feature_config` (`feature_code`, `use_default`, `enabled`)
SELECT 'briefing', 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_feature_config` WHERE `feature_code` = 'briefing');

INSERT INTO `t_ai_feature_config` (`feature_code`, `use_default`, `enabled`)
SELECT 'question_review', 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_feature_config` WHERE `feature_code` = 'question_review');
