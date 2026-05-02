package cn.org.alan.exam.utils.security;


import cn.org.alan.exam.model.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * 适配 Spring Security {@link UserDetails} 的登录主体：包装领域用户 {@link User} 与 GrantedAuthority 列表，
 * 供 {@link org.springframework.security.authentication.UsernamePasswordAuthenticationToken} 及会话外 JWT 流程使用。
 *
 * @author haoxr
 */
@Data
@NoArgsConstructor
public class SysUserDetails implements UserDetails {
    /** JWT/数据库加载的角色或权限，映射为 {@link SimpleGrantedAuthority}。 */
    private List<SimpleGrantedAuthority> permissions;
    /** 持久化用户实体（含 id、班级、密码哈希等）。 */
    private User user;
    /** 预留字段；实际用户名来自 {@link #getUsername()} → {@code user.userName}。 */
    private String username;

    /**
     * @param user 登录成功后挂载的业务用户
     */
    public SysUserDetails(User user) {
        this.user = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return permissions;
    }

    /** 由认证流程注入权限列表。 */
    public void setPermissions(List<SimpleGrantedAuthority> permissions) {
        this.permissions = permissions;
    }

    /**
     * 返回密码哈希并在返回后将内存中 {@link User#password} 置空，降低日志或调试时泄露风险。
     */
    @Override
    public String getPassword() {
        String myPassword=user.getPassword();
        user.setPassword("");
        return myPassword;
    }

    /** 使用业务字段 {@code userName} 作为 Security 用户名。 */
    @Override
    public String getUsername() {
        return user.getUserName();
    }

    /** 未接入账户过期模型，恒为可用。 */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /** 未单独维护锁定状态，恒为未锁定。 */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /** 未单独维护凭证过期策略，恒为有效。 */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /** 未与业务启用标志联动，恒为启用。 */
    @Override
    public boolean isEnabled() {
        return true;
    }
}
