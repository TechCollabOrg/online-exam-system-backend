-- 用户表：学生专业（注册、个人中心、用户管理展示）
-- 若列已存在会报 Duplicate column，可跳过。
ALTER TABLE t_user
    ADD COLUMN major VARCHAR(64) NULL DEFAULT NULL COMMENT '专业（学生）' AFTER grade_id;
