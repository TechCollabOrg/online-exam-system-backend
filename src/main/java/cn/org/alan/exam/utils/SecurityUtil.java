package cn.org.alan.exam.utils;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.utils.security.SysUserDetails;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

/**
 * 从 Spring Security {@link org.springframework.security.core.context.SecurityContext} 读取当前登录用户信息的工具类。
 * 依赖 {@link SysUserDetails} 作为 {@link org.springframework.security.core.Authentication#getPrincipal()}。
 *
 * @author WeiJin
 */
@Slf4j
public class SecurityUtil {

    /**
     * 获取当前登录用户主键 ID（来自 JWT 落地后的 {@link cn.org.alan.exam.model.entity.User}）。
     *
     * @return 用户 id
     */
    public static Integer getUserId() {
        SysUserDetails user = (SysUserDetails) (SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return user.getUser().getId();
    }

    /**
     * 获取当前用户第一个 GrantedAuthority 字符串（如 {@code role_student}），与 JWT 中权限列表一致。
     *
     * @return Spring Security 角色标识字符串
     */
    public static String getRole() {
        List<? extends GrantedAuthority> list = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().collect(java.util.stream.Collectors.toList());
        return list.get(0).toString();
    }

    /**
     * 将 Spring 角色名映射为业务数字码：1 学生、2 教师、3 管理员；未知角色抛 {@link ServiceRuntimeException}。
     *
     * @return 角色码
     */
    public static Integer getRoleCode() {
        List<? extends GrantedAuthority> list = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream().collect(java.util.stream.Collectors.toList());
        String roleName = list.get(0).toString();
        Integer roleCode;
        if ("role_admin".equals(roleName)) {
            roleCode = 3;
        } else if ("role_teacher".equals(roleName)) {
            roleCode = 2;
        } else if ("role_student".equals(roleName)) {
            roleCode = 1;
        } else {
            throw new ServiceRuntimeException("无法获取角色代码");
        }
        return roleCode;
    }

    /**
     * 获取当前用户关联的班级 ID（学生通常非空；教师/管理员可能为空）。
     *
     * @return 班级 id，可能为 null
     */
    public static Integer getGradeId() {
        SysUserDetails user = (SysUserDetails) (SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        return user.getUser().getGradeId();
    }


}
