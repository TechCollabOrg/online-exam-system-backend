package cn.org.alan.exam.utils.agent;

import cn.org.alan.exam.common.enums.AiFeatureCode;
import cn.org.alan.exam.common.exception.ServiceRuntimeException;
import cn.org.alan.exam.model.dto.LlmResolvedConfig;
import cn.org.alan.exam.model.form.ai.AiChatHistoryItemForm;
import cn.org.alan.exam.service.IAiPlatformConfigService;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    /**
     * AI 助手专用：带 RAG 系统提示与多轮历史。
     */
    public String getAssistantChatResponse(String systemPrompt, String userMessage,
                                           List<AiChatHistoryItemForm> history) throws Exception {
        List<AiChatHistoryItemForm> safeHistory = history != null ? trimHistory(history) : Collections.emptyList();
        LlmResolvedConfig db = aiPlatformConfigService.resolveForFeature(AiFeatureCode.ASSISTANT);
        if (db != null) {
            return llmChatExecutor.chatWithHistory(
                    db, systemPrompt, safeHistory, userMessage, Constants.assistantTemperature);
        }
        String combined = buildCombinedUserMessage(safeHistory, userMessage);
        return delegate().getChatResponse(systemPrompt, combined);
    }

    private List<AiChatHistoryItemForm> trimHistory(List<AiChatHistoryItemForm> history) {
        int max = 20;
        if (history.size() <= max) {
            return history;
        }
        return new ArrayList<>(history.subList(history.size() - max, history.size()));
    }

    private String buildCombinedUserMessage(List<AiChatHistoryItemForm> history, String userMessage) {
        if (history == null || history.isEmpty()) {
            return userMessage;
        }
        StringBuilder sb = new StringBuilder("【对话历史】\n");
        for (AiChatHistoryItemForm item : history) {
            if (item == null || StringUtils.isBlank(item.getContent())) {
                continue;
            }
            String roleLabel = "assistant".equalsIgnoreCase(item.getRole()) ? "助手" : "用户";
            sb.append(roleLabel).append("：").append(item.getContent()).append("\n");
        }
        sb.append("\n【当前问题】\n").append(userMessage);
        return sb.toString();
    }

    @Override
    public String getChatResponse(String systemPrompt, String userMessage) throws Exception {
        LlmResolvedConfig db = aiPlatformConfigService.resolveForFeature(AiFeatureCode.ASSISTANT);
        if (db != null) {
            return llmChatExecutor.chat(db, systemPrompt, userMessage, Constants.temperature);
        }
        return delegate().getChatResponse(systemPrompt, userMessage);
    }

    @Override
    public String getGradingResponse(String systemPrompt, String userMessage) throws Exception {
        LlmResolvedConfig db = aiPlatformConfigService.resolveForFeature(AiFeatureCode.GRADING);
        if (db != null) {
            return llmChatExecutor.chat(db, systemPrompt, userMessage, Constants.gradingTemperature);
        }
        return delegate().getGradingResponse(systemPrompt, userMessage);
    }

    /**
     * 成绩简报等场景：使用「成绩简报」功能配置。
     */
    public String getBriefingResponse(String systemPrompt, String userMessage) throws Exception {
        LlmResolvedConfig db = aiPlatformConfigService.resolveForFeature(AiFeatureCode.BRIEFING);
        if (db != null) {
            return llmChatExecutor.chat(db, systemPrompt, userMessage, Constants.temperature);
        }
        return delegate().getChatResponse(systemPrompt, userMessage);
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
