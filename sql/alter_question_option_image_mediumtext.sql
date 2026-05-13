-- 题干/选项支持多张图片 URL 拼接（###），需更长字段；已有库执行本脚本一次即可。
ALTER TABLE `t_question` MODIFY COLUMN `image` mediumtext COLLATE utf8mb4_bin COMMENT '试题图片（单张 URL 或多张以 ### 拼接）';
ALTER TABLE `t_option` MODIFY COLUMN `image` mediumtext COLLATE utf8mb4_bin COMMENT '选项图片（单张 URL 或多张以 ### 拼接）';
