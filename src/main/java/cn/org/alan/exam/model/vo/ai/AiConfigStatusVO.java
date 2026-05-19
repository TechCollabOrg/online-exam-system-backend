package cn.org.alan.exam.model.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 非管理员可读的 AI 配置状态（不含密钥）。
 */
@Data
@ApiModel("AI 配置状态")
public class AiConfigStatusVO {

    @ApiModelProperty("是否已由管理员配置并启用")
    private Boolean configured;

    @ApiModelProperty("当前模型 ID（未配置时为空）")
    private String modelName;
}
