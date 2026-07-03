package cn.org.alan.exam.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("t_ai_feature_config")
public class AiFeatureConfig implements Serializable {

    @TableId(value = "feature_code", type = IdType.INPUT)
    private String featureCode;

    private Integer useDefault;

    private String baseUrl;

    private String apiKey;

    private String modelName;

    private Integer enabled;

    private Integer updateUserId;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
