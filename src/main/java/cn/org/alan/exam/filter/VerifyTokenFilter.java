package cn.org.alan.exam.filter;

import cn.org.alan.exam.model.entity.User;
import cn.org.alan.exam.utils.security.SysUserDetails;
import cn.org.alan.exam.utils.JwtUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.Resource;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 校验过滤器：每个请求最多执行一次（{@link OncePerRequestFilter}）。
 * <p>
 * 流程概要：读取请求头 {@code Authorization}（大小写不敏感；支持 {@code Bearer} 前缀，忽略大小写）→
 * 调用 {@link JwtUtil#verifyAndRefreshToken(String)} 校验并可能在临近过期时续签 → 将 JWT 中的用户与权限
 * 反序列化后写入 {@link SecurityContextHolder}，供 {@code @PreAuthorize} 等方法级鉴权使用。
 * 若 token 缺失或校验失败，则放行交由后续链路处理（由 {@link cn.org.alan.exam.config.SecurityConfig} 的
 * {@code authenticationEntryPoint} 等对未认证请求统一返回）。
 * </p>
 * <p>
 * 说明：学生端 Electron 常以 {@code file://} 打开页面，JSESSIONID 可能不稳定；鉴权以 JWT 为准，
 * 不再要求 Redis 中 {@code token:{sessionId}} 与请求头一致（Redis 仅作续签后的兼容写入）。
 * </p>
 *
 * @author WeiJin
 */
@Slf4j
@Component
public class VerifyTokenFilter extends OncePerRequestFilter {
    private static final int BEARER_LEN = "Bearer ".length();

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String rawHeader = resolveAuthorizationHeader(request);
        if (StringUtils.isBlank(rawHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = stripBearerPrefix(rawHeader.trim());
        if (StringUtils.isBlank(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshedToken = jwtUtil.verifyAndRefreshToken(token);
        if (refreshedToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!refreshedToken.equals(token)) {
            try {
                stringRedisTemplate.opsForValue().set(
                        "token:" + request.getSession().getId(), refreshedToken, 30, TimeUnit.MINUTES);
            } catch (Exception e) {
                log.warn("续签后写入 Redis 会话 token 失败（不影响本次 JWT 鉴权）: {}", e.getMessage());
            }
            response.setHeader("Authorization", "Bearer " + refreshedToken);
        }

        try {
            String userInfo = jwtUtil.getUser(refreshedToken);
            if (StringUtils.isBlank(userInfo)) {
                log.warn("JWT 中 userInfo 为空，跳过安全上下文注入");
                filterChain.doFilter(request, response);
                return;
            }
            List<String> authList = jwtUtil.getAuthList(refreshedToken);
            if (authList == null) {
                authList = Collections.emptyList();
            }

            User sysUser = objectMapper.readValue(userInfo, User.class);
            List<SimpleGrantedAuthority> permissions = authList.stream()
                    .filter(StringUtils::isNotBlank)
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            SysUserDetails securityUser = new SysUserDetails(sysUser);
            securityUser.setPermissions(permissions);

            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(securityUser, null, permissions);
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
        } catch (Exception e) {
            log.warn("解析 JWT 并注入安全上下文失败: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Servlet 规范下 {@link HttpServletRequest#getHeader(String)} 对名称大小写不敏感；此处仍兼容部分网关
     * 只转发小写 {@code authorization} 的场景。
     */
    private static String resolveAuthorizationHeader(HttpServletRequest request) {
        String v = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(v)) {
            return v;
        }
        return request.getHeader("authorization");
    }

    /**
     * 去掉 Bearer 前缀（忽略大小写、忽略首尾空格）；无前缀则返回已 trim 的原串。
     */
    private static String stripBearerPrefix(String value) {
        if (value == null) {
            return null;
        }
        String v = value.trim();
        if (v.length() >= BEARER_LEN && v.regionMatches(true, 0, "Bearer ", 0, BEARER_LEN)) {
            return v.substring(BEARER_LEN).trim();
        }
        return v;
    }
}
