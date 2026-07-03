-- 选项解析支持富文本 HTML + 多张内嵌图，建议用 MEDIUMTEXT。若此前已执行 alter_t_option_analysis.sql 且列为 text，执行本脚本一次即可。
ALTER TABLE `t_option`
  MODIFY COLUMN `analysis` mediumtext COLLATE utf8mb4_bin COMMENT '该选项答案解析（HTML，可内嵌多图；刷题预习接口不返回）';
