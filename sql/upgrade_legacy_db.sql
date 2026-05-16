-- =============================================================================
-- 旧版 db_exam 升级到当前代码所需结构（已有库执行一次，按顺序执行）
-- 若某列已存在会报 Duplicate column，可跳过对应语句。
-- =============================================================================

-- 选项表：选项级解析（JSON/Excel 导入会写入）
SOURCE alter_t_option_analysis.sql;

-- 题目表：复合题 sub_items JSON
SOURCE alter_question_sub_items.sql;

-- 题目表：材料题 parent_qu_id
SOURCE alter_question_parent_qu_id.sql;

-- 可选：题干/选项/图片字段扩为 MEDIUMTEXT（大段 HTML、Base64 图）
-- SOURCE alter_t_question_content_mediumtext.sql;
-- SOURCE alter_t_option_content_mediumtext.sql;
-- SOURCE alter_question_option_image_mediumtext.sql;
-- SOURCE alter_t_option_analysis_mediumtext.sql;
