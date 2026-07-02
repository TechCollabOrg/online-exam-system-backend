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

-- 作答表：单题得分 score（阅卷、考后详情需要）
SOURCE alter_t_exam_qu_answer_score.sql;

-- 考试表：复合题数量与默认分值（考试管理、组卷需要）
SOURCE alter_t_exam_compound_type.sql;

-- 可选：题干/选项/图片字段扩为 MEDIUMTEXT（大段 HTML、Base64 图）
-- SOURCE alter_t_question_content_mediumtext.sql;
-- SOURCE alter_t_option_content_mediumtext.sql;
-- SOURCE alter_question_option_image_mediumtext.sql;
-- SOURCE alter_t_option_analysis_mediumtext.sql;

-- 注册邀请码（教师/管理员注册须凭码）
-- SOURCE create_t_invite_code.sql;

-- 题库知识树（AI 生成知识点层级）
SOURCE alter_t_repo_knowledge_tree.sql;

-- 分值库存格式：展示分 × 100（支持 0.5 分等小数；旧库执行一次）
SOURCE alter_score_storage_x100.sql;

-- 用户表：学生专业
SOURCE alter_t_user_major.sql;

-- 按学生发布考试
SOURCE alter_t_exam_user.sql;

-- 题目表：听力音频
SOURCE alter_t_question_audio.sql;
