package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
@ApiModel("分功能 AI 配置保存")
public class AiFeatureConfigForm {

    @NotNull(message = "请指定是否使用默认连接")
    @ApiModelProperty("true=沿用默认连接")
    private Boolean useDefault;

    @ApiModelProperty("单独配置时的基础 URL")
    private String baseUrl;

    @ApiModelProperty("单独配置时的 API 密钥，留空表示不修改")
    private String apiKey;

    @ApiModelProperty("单独配置时的模型")
    private String modelName;

    @ApiModelProperty("单独配置时是否启用")
    private Boolean enabled;
}
