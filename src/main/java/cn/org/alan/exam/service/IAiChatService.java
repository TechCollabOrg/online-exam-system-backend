package cn.org.alan.exam.service;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiChatForm;
import cn.org.alan.exam.model.vo.ai.AiChatReplyVO;

/**
 * 面向前端的 AI 对话：复用 {@link cn.org.alan.exam.utils.agent.AIChat} 实现（由 {@code online-exam.chat-platform.type} 选择厂商）。
 */
public interface IAiChatService {

    /**
     * 发送用户消息，返回平台答复文本。
     *
     * @param form 用户输入
     * @return 成功时 {@code data.reply} 为答复；失败时 {@code code=0}
     */
    Result<AiChatReplyVO> chat(AiChatForm form);
}
