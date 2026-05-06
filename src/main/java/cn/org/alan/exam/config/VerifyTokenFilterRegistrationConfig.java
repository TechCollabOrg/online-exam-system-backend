package cn.org.alan.exam.config;

import cn.org.alan.exam.filter.VerifyTokenFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 禁止将 {@link VerifyTokenFilter} 重复注册为 Servlet 容器级 Filter。
 * <p>
 * 该类已通过 {@link SecurityConfig} 挂入 Spring Security 过滤器链；若再被 Boot 自动注册为独立 Filter，
 * 在部分部署顺序下可能与 {@code SecurityContextPersistenceFilter} 产生竞态，导致 JWT 已写入上下文
 * 随后又被空会话覆盖，桌面端（Electron / 无稳定 JSESSIONID）表现为登录后仍匿名访问。
 * </p>
 */
@Configuration
public class VerifyTokenFilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<VerifyTokenFilter> disableVerifyTokenServletRegistration(VerifyTokenFilter filter) {
        FilterRegistrationBean<VerifyTokenFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
