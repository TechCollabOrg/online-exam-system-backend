package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@ApiModel("保存 AI 平台配置")
public class AiPlatformConfigForm {

    @NotBlank(message = "请填写 API 基础地址")
    private String baseUrl;

    @ApiModelProperty("留空表示不修改已保存的密钥")
    private String apiKey;

    @NotBlank(message = "请选择或填写模型")
    private String modelName;

    @ApiModelProperty("是否启用")
    private Boolean enabled;
}
