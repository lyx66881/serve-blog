package com.blog.websocket;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 私信 WebSocket 处理器：仅负责服务端向在线用户推送（发消息走 REST）。
 * <p>
 * 握手时 JwtHandshakeInterceptor 已校验 token 并把 userId 写入 session attributes；
 * 客户端心跳：发送 {"type":"ping"}，服务端回 {"type":"pong"}。
 */
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    /** 握手后 session attributes 中存放的用户 ID key */
    public static final String ATTR_USER_ID = "userId";

    private final Map<Long, Set<WebSocketSession>> onlineSessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userIdOf(session);
        if (userId == null) {
            closeQuietly(session);
            return;
        }
        onlineSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
        log.info("WS connected userId={} online={}", userId, sessionCounter());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userIdOf(session);
        if (userId == null) {
            return;
        }
        Set<WebSocketSession> set = onlineSessions.get(userId);
        if (set != null) {
            set.remove(session);
            if (set.isEmpty()) {
                onlineSessions.remove(userId, set);
            }
        }
        log.info("WS closed userId={} status={}", userId, status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        if ("{\"type\":\"ping\"}".equals(message.getPayload().trim())) {
            try {
                session.sendMessage(new TextMessage("{\"type\":\"pong\"}"));
            } catch (IOException ignored) {
                // 连接异常由 afterConnectionClosed 兜底清理
            }
        }
    }

    /**
     * 向某用户的所有在线连接推送文本；用户离线时静默跳过（消息已落库，上线后拉取）。
     */
    public void pushToUser(Long userId, String json) {
        Set<WebSocketSession> set = onlineSessions.get(userId);
        if (set == null || set.isEmpty()) {
            return;
        }
        for (WebSocketSession session : set) {
            try {
                synchronized (session) {
                    if (session.isOpen()) {
                        session.sendMessage(new TextMessage(json));
                    }
                }
            } catch (Exception e) {
                log.warn("WS push failed userId={}", userId, e);
            }
        }
    }

    private Long userIdOf(WebSocketSession session) {
        Object userId = session.getAttributes().get(ATTR_USER_ID);
        return userId instanceof Long id ? id : null;
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            session.close(CloseStatus.POLICY_VIOLATION);
        } catch (IOException ignored) {
            // 忽略关闭异常
        }
    }

    private int sessionCounter() {
        return onlineSessions.values().stream().mapToInt(Set::size).sum();
    }
}
