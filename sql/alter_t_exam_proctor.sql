-- 监考功能：考试表扩展 + 事件/暂离/在线心跳表（仅需执行一次；若列已存在请跳过对应 ALTER）

ALTER TABLE `t_exam`
    ADD COLUMN `proctor_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '1 启用摄像头监考' AFTER `max_count`,
    ADD COLUMN `allow_leave` TINYINT NOT NULL DEFAULT 0 COMMENT '1 允许暂离' AFTER `proctor_enabled`,
    ADD COLUMN `leave_max_minutes` INT NOT NULL DEFAULT 5 COMMENT '单次暂离最长分钟' AFTER `allow_leave`,
    ADD COLUMN `leave_max_count` INT NOT NULL DEFAULT 1 COMMENT '整场最多暂离次数' AFTER `leave_max_minutes`;

CREATE TABLE IF NOT EXISTS `t_proctor_event` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `exam_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `event_type` VARCHAR(32) NOT NULL,
    `detail` VARCHAR(512) DEFAULT NULL,
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_proctor_event_exam` (`exam_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监考告警事件';

CREATE TABLE IF NOT EXISTS `t_proctor_leave` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `exam_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `minutes` INT NOT NULL,
    `start_time` DATETIME NOT NULL,
    `expected_end` DATETIME NOT NULL,
    `actual_end` DATETIME DEFAULT NULL,
    `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0 暂离中 1 已返回 2 超时',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_proctor_leave_exam_user` (`exam_id`, `user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监考暂离记录';

CREATE TABLE IF NOT EXISTS `t_proctor_presence` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `exam_id` INT NOT NULL,
    `user_id` INT NOT NULL,
    `last_heartbeat` DATETIME NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_proctor_presence` (`exam_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='监考在线心跳';
