package cn.org.alan.exam.model.form.ai;

import io.swagger.annotations.ApiModel;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@ApiModel("管理员发送测试消息")
public class AiConfigTestChatForm {

    @NotBlank(message = "请输入测试内容")
    @Size(max = 500, message = "测试内容不超过 500 字")
    private String message;
}
