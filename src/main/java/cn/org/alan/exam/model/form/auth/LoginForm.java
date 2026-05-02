package cn.org.alan.exam.model.form.auth;

import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 用户名密码登录及验证码等字段。
 *
 * @author Alan
 * @since 2024/5/6
 */
@Data
public class LoginForm {

    // 用户名
    @NotBlank(message = "用户名不能为空")
    private String username;

    // 密码
    @NotBlank(message = "密码不能为空")
    private String password;
}
