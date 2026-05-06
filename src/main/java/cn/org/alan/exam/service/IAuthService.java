package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.auth.LoginForm;
import cn.org.alan.exam.model.form.auth.VerifyCodeForm;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.auth.CaptchaVO;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * 认证与会话：登录、注销、验证码、修改密码等（非 Spring Security 角色配置）。
 *
 * @author Alan
 * @since 2024/3/28
 */
public interface IAuthService {

    /**
     * 登录
     *
     * @param request
     * @param loginForm
     * @return
     */
    Result<String> login(HttpServletRequest request, LoginForm loginForm);

    /**
     * 用户注销
     *
     * @param request request对象，需要清除session里面的内容
     * @return 响应结果
     */
    Result<String> logout(HttpServletRequest request);

    /**
     * 获取图片验证码
     *
     * @param request  request对象，获取sessionId
     * @param response response对象，响应图片
     */
    void getCaptcha(HttpServletRequest request, HttpServletResponse response);

    /**
     * 获取图片验证码（JSON，含 captchaId 与 Base64 图）：与 axios 同通道，避免 img 与 XHR 会话不一致。
     *
     * @param request 当前请求（用于创建会话，供后续 isVerifyCode 写入）
     * @return captchaId + imageBase64
     */
    Result<CaptchaVO> getCaptchaJson(HttpServletRequest request);

    /**
     * 校验验证码（依赖 {@link #getCaptchaJson} 返回的 captchaId 与当前会话）。
     *
     * @param request 当前请求
     * @param form    验证码与 captchaId
     * @return 响应结果
     */
    Result<String> verifyCode(HttpServletRequest request, VerifyCodeForm form);

    /**
     * 用户注册，只能注册学生
     *
     * @param request  request对象，用于获取sessionId
     * @param userForm 用户信息
     * @return 响应结果
     */
    Result<String> register(HttpServletRequest request, UserForm userForm);

    /**
     * 记录学生登录时间
     *
     * @param request
     * @return
     */
    Result<String> sendHeartbeat(HttpServletRequest request);
}