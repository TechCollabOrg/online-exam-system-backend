-- 试题听力音频：刷题/考试拉取单题详情会查询 audio 列，旧库缺失会导致 /api/exercises/question/{id} 报 500、刷题无法提交
ALTER TABLE `t_question`
  ADD COLUMN `audio` varchar(1024) NULL COMMENT '试题音频（如英语听力；单条 URL 或多条以 ### 拼接）' AFTER `image`;
