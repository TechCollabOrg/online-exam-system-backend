package cn.org.alan.exam.utils.agent.impl;

import java.util.Collections;
import java.util.Optional;

import cn.org.alan.exam.utils.agent.AIChat;
import com.coze.openapi.client.chat.*;
import com.coze.openapi.client.chat.model.ChatPoll;
import com.coze.openapi.client.connversations.message.model.Message;
import com.coze.openapi.service.auth.TokenAuth;
import com.coze.openapi.service.service.CozeAPI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * {@link AIChat} 的扣子（Coze）实现：使用 PAT 鉴权调用 {@link CozeAPI}，对指定 Bot 发起对话并
 * {@code createAndPoll} 直至结束，从消息列表中取首条类型为 {@code answer} 的文本。
 * <p>
 * 配置 {@code online-exam.chat-platform.type=coze} 时生效；{@link #uid} 为调用侧用户标识占位，可按业务改为真实用户 ID。
 *
 * @author 赵浩森
 * @since 2025/4/12
 */
@Service
@ConditionalOnProperty(name = "online-exam.chat-platform.type", havingValue = "coze")
public class CozeUtil implements AIChat {

    @Value("${coze.api.token}")
    private String token;

    @Value("${coze.api.bot}")
    private String botID;

    @Value("${coze.api.base-url}")
    private String baseUrl;

    /** 会话维度用户标识，需与 Coze 控制台及应用约定一致时可替换为业务用户 ID。 */
    private String uid = "USER_ID";

    /**
     * 创建会话并轮询直到结束，取类型为 {@code answer} 的首条消息内容。
     */
    @Override
    public String getChatResponse(String msg) throws Exception {
        TokenAuth authCli = new TokenAuth(token);

        CozeAPI coze =
                new CozeAPI.Builder()
                        .baseURL(baseUrl)
                        .auth(authCli)
                        .readTimeout(10000)
                        .build();

        CreateChatReq req =
                CreateChatReq.builder()
                        .botID(botID)
                        .userID(uid)
                        .messages(Collections.singletonList(Message.buildUserQuestionText(msg)))
                        .build();

        ChatPoll chat = coze.chat().createAndPoll(req);

        final Optional<String> answer = chat.getMessages().stream()
                .filter(message -> message.getType().getValue().equals("answer"))
                .map(Message::getContent)
                .findFirst();

        return answer.orElseThrow(() -> new RuntimeException("No answer found"));
    }

    @Override
    public String getChatResponse(String systemPrompt, String userMessage) throws Exception {
        return getChatResponse(systemPrompt + "\n\n" + userMessage);
    }
}