package cn.org.alan.exam;


import com.github.xiaoymin.knife4j.spring.annotations.EnableKnife4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * 在线考试系统 —— Spring Boot 启动入口。
 * <p>
 * 与《需求分析文档》的对应关系（实现分散在各模块，此处仅作索引）：
 * </p>
 * <ul>
 *   <li>5.2 安全性 / JWT：自定义 {@code SecurityConfig} + Token 过滤器，排除默认 SecurityAutoConfiguration 后自行装配。</li>
 *   <li>5.4 消息与会话：{@code @EnableRedisHttpSession} 将会话存 Redis，便于集群与令牌协同（maxInactiveInterval 为秒）。</li>
 *   <li>2.3 考后定时任务：{@code @EnableScheduling} 配合 {@code cn.org.alan.exam.task} 包内任务（如考试状态流转）。</li>
 *   <li>5.2 AI 降级等异步：{@code @EnableAsync} 用于非阻塞调用外部 AI（具体超时与降级见阅卷相关 Service）。</li>
 * </ul>
 * <p>启动前请配置 {@code application-dev.yml} 中 MySQL、Redis、MinIO 等，并导入 {@code lib} 目录下 SQL 初始化库表。</p>
 *
 * @author Alan
 * @version 1.0
 * @date 2025/3/25 11:20 AM
 */
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class})
@EnableAsync
@EnableScheduling
@EnableKnife4j
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800 * 2 )
public class ExamApplication{

    /**
     * 程序入口：启动内嵌 Tomcat，加载 application.yml 中指定的 profile（默认 dev）。
     */
    public static void main(String[] args) {
        SpringApplication.run(ExamApplication.class, args);
    }
}