package cn.org.alan.exam.model.vo.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * AI 对话接口返回的正文载荷。
 */
@Data
@ApiModel("AI 对话回复")
public class AiChatReplyVO {

    @ApiModelProperty("模型或编排平台返回的纯文本")
    private String reply;
}
