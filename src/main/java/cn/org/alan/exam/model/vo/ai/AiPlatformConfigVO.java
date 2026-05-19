package cn.org.alan.exam.model.vo.ai;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员查看的配置（密钥脱敏）。
 */
@Data
@ApiModel("AI 平台配置（管理端）")
public class AiPlatformConfigVO {

    private String baseUrl;

    @ApiModelProperty("是否已保存密钥（不回传明文）")
    private Boolean apiKeySet;

    private String modelName;

    private Boolean enabled;

    private Boolean lastTestOk;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTestTime;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
