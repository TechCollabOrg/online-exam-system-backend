-- =============================================================================
-- 升级脚本（独立执行，勿改 db_exam.sql）
-- 用途：整题解析改为富文本后可存 HTML + 内嵌图，原 text 列易报 Data too long
-- 执行：在业务库执行本文件一次即可（与 alter_t_option_analysis_mediumtext.sql 类似）
-- =============================================================================

ALTER TABLE `t_question`
  MODIFY COLUMN `analysis` mediumtext COLLATE utf8mb4_bin COMMENT '整题解析（HTML，可内嵌多图）';
