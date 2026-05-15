-- 题干可为富文本 HTML + 多图，建议 MEDIUMTEXT。已有库执行一次即可。
ALTER TABLE `t_question`
  MODIFY COLUMN `content` mediumtext COLLATE utf8mb4_bin NOT NULL COMMENT '题干（可为含多图的 HTML）';
