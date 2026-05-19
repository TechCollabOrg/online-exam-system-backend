package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 测试连接 / 拉取模型时可传入临时端点与密钥。
 */
@Data
@ApiModel("探测 AI 连接参数")
public class AiPlatformConfigProbeForm {

    @NotBlank(message = "请填写 API 基础地址")
    private String baseUrl;

    @ApiModelProperty("留空则使用库中已保存的密钥")
    private String apiKey;
}
