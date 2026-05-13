-- 客观题每个选项可录入文字解析（与整题「题目分析」并存）。已有库执行一次即可。
ALTER TABLE `t_option`
  ADD COLUMN `analysis` mediumtext COLLATE utf8mb4_bin COMMENT '该选项答案解析（HTML，可内嵌多图；刷题预习接口不返回本列）' AFTER `content`;
