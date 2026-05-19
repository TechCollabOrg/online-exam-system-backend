package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 管理员维护的 OpenAI 兼容 API 连接配置（单例，id 固定为 1）。
 */
@Data
@ApiModel("AI 平台连接配置")
@TableName("t_ai_platform_config")
public class AiPlatformConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int SINGLETON_ID = 1;

    @TableId(value = "id", type = IdType.INPUT)
    private Integer id;

    @ApiModelProperty("OpenAI 兼容基础 URL")
    private String baseUrl;

    @ApiModelProperty("API 密钥")
    private String apiKey;

    @ApiModelProperty("模型 ID")
    private String modelName;

    @ApiModelProperty("是否启用：1 是 0 否")
    private Integer enabled;

    @ApiModelProperty("上次连接测试是否成功")
    private Integer lastTestOk;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastTestTime;

    private Integer updateUserId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
