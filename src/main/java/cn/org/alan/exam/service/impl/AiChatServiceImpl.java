package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiChatForm;
import cn.org.alan.exam.model.vo.ai.AiChatReplyVO;
import cn.org.alan.exam.service.AiKnowledgeRagService;
import cn.org.alan.exam.service.IAiChatService;
import cn.org.alan.exam.utils.agent.AIChatRouter;
import cn.org.alan.exam.utils.agent.Constants;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;

/**
 * AI 助手对话：RAG 检索操作说明 + 专用系统提示，不访问题库与答卷。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private AIChatRouter aiChatRouter;

    @Resource
    private AiKnowledgeRagService aiKnowledgeRagService;

    @Override
    public Result<AiChatReplyVO> chat(AiChatForm form) {
        try {
            String ragContext = aiKnowledgeRagService.retrieveContext(form.getMessage());
            String systemPrompt = Constants.buildAssistantSystemMessage(ragContext);
            String text = aiChatRouter.getAssistantChatResponse(
                    systemPrompt,
                    form.getMessage(),
                    form.getHistory() != null ? form.getHistory() : Collections.emptyList());
            AiChatReplyVO vo = new AiChatReplyVO();
            vo.setReply(text != null ? text : "");
            return Result.success("ok", vo);
        } catch (Exception e) {
            return Result.failed("AI 调用失败：" + e.getMessage());
        }
    }
}
