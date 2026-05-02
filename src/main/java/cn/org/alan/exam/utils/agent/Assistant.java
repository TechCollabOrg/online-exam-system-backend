package cn.org.alan.exam.utils.agent;

/**
 * LangChain4j AI Service 接口：由 {@link dev.langchain4j.service.AiServices} 生成代理实现，
 * 用于将聊天模型封装为普通 Spring 可调用的服务方法。
 * <p>
 * 官方说明见 <a href="https://docs.langchain4j.dev/tutorials/ai-services">LangChain4j AI Services</a>。
 *
 * @author 赵浩森
 */
public interface Assistant {

    /**
     * 根据拼接后的提示词（可含系统人设与用户输入）生成模型回复文本。
     *
     * @param query 完整提示内容
     * @return 模型输出字符串
     */
    String answer(String query);
}