package cn.org.alan.exam.utils.agent;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.stereotype.Component;

/**
 * 基于 OpenAI 兼容接口的聊天执行器，供管理员配置与业务 AI 功能共用。
 */
@Component
public class LlmChatExecutor {

    public String chat(LlmResolvedConfig config, String systemPrompt, String userMessage, Double temperature) {
        if (config == null || StringUtils.isBlank(config.getApiKey())) {
            throw new ServiceRuntimeException("AI 接口未配置或密钥为空");
        }
        if (StringUtils.isBlank(config.getModelName())) {
            throw new ServiceRuntimeException("未选择模型");
        }
        String baseUrl = LlmConnectionHelper.normalizeBaseUrl(config.getBaseUrl());
        OpenAiChatModel llm = OpenAiChatModel.builder()
                .apiKey(config.getApiKey().trim())
                .modelName(config.getModelName().trim())
                .baseUrl(baseUrl)
                .temperature(temperature != null ? temperature : Constants.temperature)
                .maxTokens(Constants.maxToken)
                .build();
        Assistant assistant = AiServices.builder(Assistant.class)
                .chatLanguageModel(llm)
                .build();
        ChatMessage systemMessage = new SystemMessage(systemPrompt);
        ChatMessage userMsg = new UserMessage(userMessage);
        String input = systemMessage.text() + "\n" + userMsg.text();
        return assistant.answer(input);
    }
}
