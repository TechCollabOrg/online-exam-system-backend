package cn.org.alan.exam.model.form.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 校验图形验证码请求体：使用 {@link #captchaId} 绑定 Redis，不依赖图片请求建立的 Session。
 */
@Data
@ApiModel("校验验证码表单")
public class VerifyCodeForm implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty(value = "用户输入的验证码", required = true)
    private String code;

    @ApiModelProperty(value = "获取验证码 JSON 时返回的 captchaId", required = true)
    private String captchaId;
}
