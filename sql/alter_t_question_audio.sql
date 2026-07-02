-- 题目表增加听力音频字段（单条 URL；多条可用 ### 拼接，与试题图片规则相同）
ALTER TABLE `t_question`
  ADD COLUMN `audio` varchar(1024) COLLATE utf8mb4_bin DEFAULT NULL COMMENT '试题音频（如英语听力；单条 URL 或多条以 ### 拼接）' AFTER `image`;
