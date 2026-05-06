package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.group.UserGroup;
import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.auth.LoginForm;
import cn.org.alan.exam.model.form.auth.VerifyCodeForm;
import cn.org.alan.exam.model.form.user.UserForm;
import cn.org.alan.exam.model.vo.auth.CaptchaVO;
import cn.org.alan.exam.service.IAuthService;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 匿名访问的认证接口：登录、注册、验证码与在线心跳；与 {@code /api/auths/**} 在 Security 中放行一致。
 *
 * @author WeiJin
 */
@Api(tags = "权限管理接口")
@RestController
@RequestMapping("/api/auths")
public class AuthController {


    @Resource
    private IAuthService iAuthService;

    @Value("${online-exam.login.captcha.enabled}")
    private boolean captchaEnabled;

    /**
     * POST 登录：Body 为 {@link LoginForm}；成功返回 JWT（同时写入 Redis 与会话相关键）。
     */
    @ApiOperation("用户登录")
    @PostMapping("/login")
    public Result<String> login(HttpServletRequest request,
                                @Validated @RequestBody LoginForm loginForm) {
        return iAuthService.login(request, loginForm);
    }

    /** DELETE 注销：清除 Redis Token 与 Session。 */
    @ApiOperation("用户注销")
    @DeleteMapping("/logout")
    public Result<String> logout(HttpServletRequest request) {
        return iAuthService.logout(request);
    }

    /** POST 学生注册：需先通过验证码校验流程；校验分组 {@link UserGroup.RegisterGroup}。 */
    @ApiOperation("用户注册")
    @PostMapping("/register")
    public Result<String> register(HttpServletRequest request,
                                   @RequestBody @Validated(UserGroup.RegisterGroup.class) UserForm userForm) {
        return iAuthService.register(request, userForm);
    }

    /** GET 输出 JPEG 验证码图片（验证码文本存 Redis，键与 Session 绑定；兼容旧客户端与文档直连）。 */
    @ApiOperation("获取图片验证码")
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletRequest request, HttpServletResponse response) {
        iAuthService.getCaptcha(request, response);
    }

    /** GET 返回 captchaId + Base64 图片，供 Web/Electron 与校验请求共用 axios 会话，避免「验证码已过期」误报。 */
    @ApiOperation("获取图片验证码JSON")
    @GetMapping("/captcha/json")
    public Result<CaptchaVO> getCaptchaJson(HttpServletRequest request) {
        if (!captchaEnabled) {
            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaId("");
            vo.setImageBase64("");
            return Result.success("验证码已关闭", vo);
        }
        return iAuthService.getCaptchaJson(request);
    }

    /**
     * POST 校验图形码：Body 为 {@link VerifyCodeForm}；全局关闭验证码时直接成功。
     */
    @ApiOperation("校验验证码")
    @PostMapping("/verifyCode")
    public Result<String> verifyCode(HttpServletRequest request, @RequestBody(required = false) VerifyCodeForm form) {
        if (!captchaEnabled) {
            return Result.success();
        }
        return iAuthService.verifyCode(request, form);
    }

    /** POST 学生端心跳/在线时长上报（仅角色为学生时累计时长）。 */
    @ApiOperation("记录学生登录时间")
    @PostMapping("/track-presence")
    public Result<String> trackPresence(HttpServletRequest request) {
        return iAuthService.sendHeartbeat(request);
    }

}
