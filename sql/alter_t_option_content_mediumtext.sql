-- 简答题参考答案存于 t_option.content，可为富文本 HTML + 多图，建议 MEDIUMTEXT。已有库执行一次即可。
ALTER TABLE `t_option`
  MODIFY COLUMN `content` mediumtext COLLATE utf8mb4_bin NOT NULL COMMENT '选项内容（简答题参考答案可为含多图的 HTML）';
