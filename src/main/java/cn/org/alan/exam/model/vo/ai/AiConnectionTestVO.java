package cn.org.alan.exam.model.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
@ApiModel("AI 连接测试结果")
public class AiConnectionTestVO {

    @ApiModelProperty("连接是否有效")
    private Boolean valid;

    @ApiModelProperty("说明信息")
    private String message;

    @ApiModelProperty("可用模型 ID 列表（连接成功时尽量返回）")
    private List<String> models;
}
