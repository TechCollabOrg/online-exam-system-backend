package cn.org.alan.exam.websocket;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JSR-356 WebSocket 端点 {@code /websocket}：按用户 ID 维护会话映射，支持广播文本消息。
 * <p>
 * 前端需在连接 URL 上携带查询参数 {@code userId=…}（见 {@link #getUserIdBySession(Session)}），
 * 同一用户重复连接时会关闭旧会话并注册新会话。
 * </p>
 *
 * @author WeiJin
 */
@ServerEndpoint("/websocket")
@Component
@Slf4j
public class WebsocketHandler {

    private static final ConcurrentHashMap<Integer, Session> SESSION_MAP = new ConcurrentHashMap<>();

    /**
     * Spring 注入占位：{@link ServerEndpoint} 实例由容器管理，此处仅用于满足组件扫描下的依赖注入钩子（当前无字段赋值）。
     */
    @Autowired
    public void setInstance() {

    }

    /**
     * 握手成功：解析 {@code userId}，若该用户尚无活跃连接则写入 {@link #SESSION_MAP}。
     *
     * @param session 当前 WebSocket 会话
     */
    @OnOpen
    public void onOpen(Session session) {
        Integer userId = getUserIdBySession(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        Session existing = SESSION_MAP.get(userId);
        if (existing != null && existing.isOpen() && !existing.getId().equals(session.getId())) {
            closeQuietly(existing);
        }
        SESSION_MAP.put(userId, session);
        log.info("[websocket消息]：用户 {} 加入连接，当前连接总数：{}", userId, SESSION_MAP.size());
    }


    /**
     * 连接关闭：根据 {@code userId} 从映射中移除（若映射中无对应会话则忽略）。
     *
     * @param session 即将关闭的会话
     */
    @OnClose
    public void onClose(Session session) {
        Integer userId = getUserIdBySession(session);
        if (userId == null) {
            return;
        }
        Session existSession = SESSION_MAP.get(userId);
        if (existSession == null || !existSession.getId().equals(session.getId())) {
            return;
        }
        SESSION_MAP.remove(userId);
        log.info("[websocket消息]：用户 {} 断开连接", userId);
    }

    /**
     * 传输或协议层错误：记录日志（不在此清理 {@link #SESSION_MAP}，关闭流程仍走 {@link #onClose(Session)}）。
     *
     * @param throwable 错误原因
     */
    @OnError
    public void onError(Throwable throwable) {
        log.error("WebSocket error: {}", throwable.getMessage());
    }

    /**
     * 收到客户端文本消息后广播给当前所有在线会话（过滤已关闭连接）。
     *
     * @param session 发送方会话（当前实现未按发送方区分，全员广播）
     * @param message 文本载荷
     */
    @OnMessage
    public void onMessage(Session session, String message) {
        // 反序列化字符串信息获取消息信息
        // Message msg = JSON.parseObject(message, Message.class);

        // 发送给所有用户
        sendAllMessage(message);
        log.info("[websocket消息]：收到消息 {}", message);

    }

    /**
     * 向 {@link #SESSION_MAP} 中所有仍处于打开状态的会话同步发送文本（类级锁保护 BasicRemote 发送）。
     *
     * @param message 广播内容
     */
    private void sendAllMessage(String message) {
        SESSION_MAP.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    synchronized (WebsocketHandler.class) {
                        session.getBasicRemote().sendText(message);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    /**
     * 从握手 URL 的 Query 串解析用户 ID（约定形如 {@code …?userId=123}，取最后一个 {@code =} 右侧整数）。
     *
     * @param session WebSocket 会话
     * @return 用户主键
     */
    private Integer getUserIdBySession(Session session) {
        String query = session.getRequestURI().getQuery();
        if (query == null || !query.contains("userId=")) {
            return null;
        }
        String[] arr = query.split("=");
        try {
            return Integer.parseInt(arr[arr.length - 1]);
        } catch (NumberFormatException e) {
            log.warn("WebSocket 握手缺少有效 userId: {}", query);
            return null;
        }
    }

    private void closeQuietly(Session session) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close();
        } catch (IOException e) {
            log.warn("关闭 WebSocket 会话失败: {}", e.getMessage());
        }
    }
}
