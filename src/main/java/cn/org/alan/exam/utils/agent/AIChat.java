package cn.org.alan.exam.utils.agent;

/**
 * 统一聊天模型调用门面：具体实现由 {@code online-exam.chat-platform.type} 选择
 * （如 {@code llm}、{@code dify}、{@code coze}），供阅卷或答疑等模块注入使用。
 *
 * @author 赵浩森
 * @since 2025/4/12
 */
public interface AIChat {

    /**
     * 发送用户侧文本，返回模型或编排平台的答复正文。
     *
     * @param msg 用户消息或拼装后的 prompt
     * @return 模型回复纯文本
     * @throws Exception HTTP、序列化或厂商 API 错误
     */
    String getChatResponse(String msg) throws Exception;

    /**
     * 使用指定系统提示词与用户消息调用模型（用于成绩简报等与阅卷不同的场景）。
     */
    String getChatResponse(String systemPrompt, String userMessage) throws Exception;
}