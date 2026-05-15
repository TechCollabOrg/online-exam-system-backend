package cn.org.alan.exam.config;

import cn.org.alan.exam.filter.VerifyTokenFilter;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.utils.ResponseUtil;

import javax.annotation.Resource;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 核心配置：基于 JWT 的无状态认证链路。
 * <ul>
 *   <li>放行登录注册、Swagger/Knife4j、部分 WebSocket 路径；其余请求需已认证。</li>
 *   <li>无权限访问资源时由 {@link cn.org.alan.exam.utils.ResponseUtil} 写入 JSON，而非默认 HTML。</li>
 *   <li>在表单登录过滤器之前插入 {@link VerifyTokenFilter}，从 Header 解析 Token 并填充 SecurityContext。</li>
 * </ul>
 *
 * @author Alan
 */
@Configuration
@EnableWebSecurity // 启用 Spring Security 的 Web 安全功能，会自动配置一个过滤器链来处理安全相关的事务
@EnableGlobalMethodSecurity(prePostEnabled = true) // 启用全局方法级别的安全控制，允许使用 @PreAuthorize 和 @PostAuthorize 等注解进行方法级别的权限验证
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    // 响应体封装工具类
    @Resource
    private ResponseUtil responseUtil;

    // 验证 token 的过滤器
    @Resource
    private VerifyTokenFilter verifyTokenFilter;

    /**
     * 组装 HTTP 安全策略：关闭 CSRF、配置匿名白名单、403 JSON、挂载 JWT 过滤器。
     *
     * @param http Spring Security 构造器
     * @throws Exception 配置异常
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        // 开启 CORS（跨域资源共享）支持，并禁用 CSRF（跨站请求伪造）保护
        http.cors().and().csrf().disable();

        // 开始配置请求的授权规则
        http.authorizeRequests()
                // 定义一系列允许匿名访问（即无需身份验证即可访问）的请求路径
                .antMatchers(
                        // 用户登录相关的接口，例如登录、注册等接口
                        "/api/auths/**",
                        // Swagger2 相关的资源路径，用于提供 API 文档的访问
                        "/webjars/**",
                        "/swagger-ui.html",
                        "/swagger-resources/**",
                        "/v2/api-docs",
                        "/swagger-resources/configuration/ui",
                        "/swagger-resources/configuration/security",
                        // 其他需要允许匿名访问的路径，如文档页面、WebSocket 相关路径等
                        "/doc.html",
                        "/websocket",
                        "/ws/**",
                        "/ws-app/**",
                        // 本地磁盘存储（storage.type=local）时，头像/题干图片通过该路径匿名读取
                        "/api/upload/files/**"
                )
                // 这些路径允许所有请求访问，无需进行身份验证
                .permitAll()
                // 除了上述允许匿名访问的路径外，其他任何请求都必须经过身份验证才能访问
                .anyRequest().authenticated();

        // 未携带有效 JWT 的受保护接口：返回 401 JSON，便于前端 axios 统一跳转登录
        // 已登录但权限不足（含 @PreAuthorize 拒绝）：返回 403 JSON
        http.exceptionHandling()
                .authenticationEntryPoint((request, response, authException) ->
                        responseUtil.response(response, Result.failed("请先登录或登录已失效"), 401))
                .accessDeniedHandler((request, response, accessDeniedException) ->
                        responseUtil.response(response, Result.failed("你没有该资源的访问权限"), 403));

        // 禁用 Spring Security 自带的基于表单的登录页面
        http.formLogin().disable();

        // 将自定义的验证 token 过滤器添加到 Spring Security 的过滤器链中，并且在 UsernamePasswordAuthenticationFilter 之前执行
        http.addFilterBefore(verifyTokenFilter, UsernamePasswordAuthenticationFilter.class);
    }
}