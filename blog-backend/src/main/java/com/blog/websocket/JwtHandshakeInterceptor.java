package com.blog.websocket;

import java.util.Map;

import com.blog.security.InMemoryTokenBlacklist;
import com.blog.security.JwtTokenProvider;

import io.jsonwebtoken.JwtException;

import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 握手鉴权：从查询参数 token 校验 JWT（浏览器 WebSocket 无法自定义请求头）。
 * 校验通过把 userId 写入 session attributes；失败返回 401 阻断握手。
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider tokenProvider;
    private final InMemoryTokenBlacklist blacklist;

    public JwtHandshakeInterceptor(JwtTokenProvider tokenProvider, InMemoryTokenBlacklist blacklist) {
        this.tokenProvider = tokenProvider;
        this.blacklist = blacklist;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank() || blacklist.contains(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        try {
            Long userId = tokenProvider.getUserId(token);
            attributes.put(ChatWebSocketHandler.ATTR_USER_ID, userId);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // 无需处理
    }
}
