-- 新增 AI 试题导入功能配置行（沿用默认 API 连接）
INSERT INTO `t_ai_feature_config` (`feature_code`, `use_default`, `enabled`)
SELECT 'question_import', 1, 0 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `t_ai_feature_config` WHERE `feature_code` = 'question_import');
