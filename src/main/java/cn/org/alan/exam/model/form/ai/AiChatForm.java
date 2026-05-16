package cn.org.alan.exam.model.form.ai;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 登录用户向当前配置的对话平台（Coze / Dify / LLM）发送的文本。
 */
@Data
public class AiChatForm {

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 8000, message = "单条消息不超过 8000 字")
    private String message;
}
