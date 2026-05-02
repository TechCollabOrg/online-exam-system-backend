package cn.org.alan.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis {@link RedisTemplate} Bean：Key/HashKey 使用字符串序列化，Value/HashValue 使用 Jackson JSON，
 * 便于存取复杂对象并与 Spring Cache、会话类存储对齐。
 *
 * @author Alan
 */
@Configuration
public class RedisConfig {

    /**
     * 构造默认 Redis 模板并绑定连接工厂；适用于 token、验证码等业务读写。
     *
     * @param redisConnectionFactory Spring Data Redis 连接工厂
     * @return 已配置序列化器的模板实例
     */
    @Bean
    public RedisTemplate redisTemplateInit(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 设置序列化Key的实例化对象
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        // 设置序列化Value的实例化对象
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        // 设置Hash类型存储时，对象序列化报错解决
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        redisTemplate.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }
}
