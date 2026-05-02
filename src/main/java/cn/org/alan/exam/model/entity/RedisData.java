package cn.org.alan.exam.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 通用 Redis 值包装：业务对象与建议过期时间，供缓存工具读写。
 *
 * @author Alan
 * @since 2024/6/9
 */
@Data
public class RedisData {
    private LocalDateTime expireTime;
    private Object data;
}
