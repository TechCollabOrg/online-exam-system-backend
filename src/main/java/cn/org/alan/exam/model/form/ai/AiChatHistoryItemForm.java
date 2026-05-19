package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@ApiModel("AI 对话历史条目")
public class AiChatHistoryItemForm {

    @NotBlank
    @Pattern(regexp = "user|assistant", message = "role 须为 user 或 assistant")
    @ApiModelProperty("user 或 assistant")
    private String role;

    @NotBlank
    @Size(max = 4000, message = "单条历史不超过 4000 字")
    private String content;
}
