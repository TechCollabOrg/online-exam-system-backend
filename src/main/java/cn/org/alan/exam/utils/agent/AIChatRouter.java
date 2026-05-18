package cn.org.alan.exam.utils.agent;

import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import cn.org.alan.exam.utils.agent.impl.CozeUtil;
import cn.org.alan.exam.utils.agent.impl.DifyUtil;
import cn.org.alan.exam.utils.agent.impl.LLMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 统一 AI 调用入口：优先使用管理员在库中配置的 OpenAI 兼容接口，否则回退到 yml（llm/coze/dify）。
 */
@Service
@Primary
public class AIChatRouter implements AIChat {

    @Resource
    private IAiPlatformConfigService aiPlatformConfigService;

    @Resource
    private LlmChatExecutor llmChatExecutor;

    @Autowired(required = false)
    private LLMUtil llmUtil;

    @Autowired(required = false)
    private CozeUtil cozeUtil;

    @Autowired(required = false)
    private DifyUtil difyUtil;

    @Override
    public String getChatResponse(String msg) throws Exception {
        return getChatResponse(Constants.systemMessage, msg);
    }

    @Override
    public String getChatResponse(String systemPrompt, String userMessage) throws Exception {
        LlmResolvedConfig db = aiPlatformConfigService.resolveActive();
        if (db != null) {
            return llmChatExecutor.chat(db, systemPrompt, userMessage, Constants.temperature);
        }
        return delegate().getChatResponse(systemPrompt, userMessage);
    }

    @Override
    public String getGradingResponse(String systemPrompt, String userMessage) throws Exception {
        LlmResolvedConfig db = aiPlatformConfigService.resolveActive();
        if (db != null) {
            return llmChatExecutor.chat(db, systemPrompt, userMessage, Constants.gradingTemperature);
        }
        return delegate().getGradingResponse(systemPrompt, userMessage);
    }

    private AIChat delegate() {
        if (llmUtil != null) {
            return llmUtil;
        }
        if (cozeUtil != null) {
            return cozeUtil;
        }
        if (difyUtil != null) {
            return difyUtil;
        }
        throw new ServiceRuntimeException("未配置 AI 接口，请联系管理员在「API 连接配置」中完成设置");
    }
}
