package cn.org.alan.exam.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Collections;

/**
 * 浏览器跨域（CORS）配置：注册全局 {@link CorsFilter}，允许任意来源与常用 HTTP 方法。
 * <p>注意：{@code allowCredentials=true} 与 {@code allowedOrigin=*} 的组合在部分浏览器规范下存在争议，生产环境建议改为白名单域名。</p>
 *
 * @author Alan
 */
@Configuration
public class CorsConfig {

    /**
     * 注册 {@link CorsFilter}，对所有路径 {@code /**} 生效：允许任意 Origin、携带 Cookie、常见方法与任意请求头。
     *
     * @return 可供 Servlet 容器注册的 CORS 过滤器
     */
    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOrigin("*");
        config.setAllowCredentials(true);
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", config);
        return new CorsFilter(configurationSource);
    }
}