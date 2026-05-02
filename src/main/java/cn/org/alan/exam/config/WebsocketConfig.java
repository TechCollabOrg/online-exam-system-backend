package cn.org.alan.exam.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;


/**
 * WebSocket 基础设施：暴露 JSR-356 {@link ServerEndpoint} 扫描注册器，并提供一个通用线程池定时器（心跳、推送等可复用）。
 *
 * @author WeiJin
 */
@Configuration
@EnableWebSocket
public class WebsocketConfig {
    /**
     * 将容器中标注 {@link javax.websocket.server.ServerEndpoint} 的 Bean 注册到 Servlet 容器。
     *
     * @return Spring WebSocket 端点导出器
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    /**
     * 调度线程池：默认 10 线程，供 Spring 调度或依赖 {@link TaskScheduler} 的组件使用。
     *
     * @return 任务调度器
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // 根据需要调整线程池大小
        return scheduler;
    }
}
