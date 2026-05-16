package cn.org.alan.exam.controller;

import cn.org.alan.exam.common.result.Result;
import cn.org.alan.exam.model.form.ai.AiChatForm;
import cn.org.alan.exam.model.vo.ai.AiChatReplyVO;
import cn.org.alan.exam.service.IAiChatService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 登录用户可用的 AI 对话入口；底层实现与主观题 {@link cn.org.alan.exam.service.impl.AutoScoringServiceImpl} 共用 {@code AIChat}。
 */
@Api(tags = "AI 助手")
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Resource
    private IAiChatService aiChatService;

    @ApiOperation("发送消息并获取 AI 回复（阻塞，可能较慢）")
    @PostMapping("/chat")
    @PreAuthorize("hasAnyAuthority('role_student','role_teacher','role_admin')")
    public Result<AiChatReplyVO> chat(@Validated @RequestBody AiChatForm form) {
        return aiChatService.chat(form);
    }
}
