package cn.org.alan.exam.service.impl;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiChatForm;
import cn.org.alan.exam.model.vo.ai.AiChatReplyVO;
import cn.org.alan.exam.service.IAiChatService;
import cn.org.alan.exam.utils.agent.AIChat;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * 将 {@link AIChat} 封装为统一业务结果，供 Controller 返回给前端。
 */
@Service
public class AiChatServiceImpl implements IAiChatService {

    @Resource
    private AIChat aiChat;

    @Override
    public Result<AiChatReplyVO> chat(AiChatForm form) {
        try {
            String text = aiChat.getChatResponse(form.getMessage());
            AiChatReplyVO vo = new AiChatReplyVO();
            vo.setReply(text != null ? text : "");
            return Result.success("ok", vo);
        } catch (Exception e) {
            return Result.failed("AI 调用失败：" + e.getMessage());
        }
    }
}
