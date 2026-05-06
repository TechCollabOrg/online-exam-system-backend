package cn.org.alan.exam.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * 浏览器跨域（CORS）配置：注册全局 {@link CorsFilter}，允许任意来源与常用 HTTP 方法。
 * <p>注意：CORS 规范下 {@code allowCredentials=true} 时不能使用 {@code Access-Control-Allow-Origin: *}，否则浏览器不会携带 Cookie。</p>
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
        // 不可使用 addAllowedOrigin("*") 与 allowCredentials(true) 同时生效；列出本地开发与 Electron file://（Origin: null）
        config.addAllowedOrigin("http://127.0.0.1:9527");
        config.addAllowedOrigin("http://localhost:9527");
        config.addAllowedOrigin("http://127.0.0.1:8080");
        config.addAllowedOrigin("http://localhost:8080");
        config.addAllowedOrigin("null");
        config.setAllowCredentials(true);
        config.addAllowedMethod("OPTIONS");
        config.addAllowedMethod("HEAD");
        config.addAllowedMethod("GET");
        config.addAllowedMethod("PUT");
        config.addAllowedMethod("POST");
        config.addAllowedMethod("DELETE");
        config.addAllowedMethod("PATCH");
        config.addAllowedHeader("*");
        // 续签 JWT 时后端在响应头返回 Authorization；浏览器 CORS 下须显式暴露，否则 axios 读不到新 Token
        config.addExposedHeader("Authorization");
        config.addExposedHeader("authorization");
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**", config);
        return new CorsFilter(configurationSource);
    }
}