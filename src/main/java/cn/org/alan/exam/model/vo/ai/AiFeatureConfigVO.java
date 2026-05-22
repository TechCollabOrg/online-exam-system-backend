package cn.org.alan.exam.model.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
@ApiModel("分功能 AI 配置")
public class AiFeatureConfigVO {

    private String featureCode;

    private String featureLabel;

    private String featureHint;

    private Boolean useDefault;

    private String baseUrl;

    private Boolean apiKeySet;

    private String modelName;

    private Boolean enabled;
}
