package cn.org.alan.exam.model.vo.auth;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;

/**
 * 图形验证码 JSON 载荷：与 {@code <img src>} 解耦，避免图片请求与 axios 会话不一致导致「验证码已过期」。
 */
@Data
@ApiModel("图形验证码")
public class CaptchaVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("一次一换的验证码标识，校验时原样回传")
    private String captchaId;

    @ApiModelProperty("JPEG 图片 Base64（不含 data: 前缀）")
    private String imageBase64;
}
