package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.chat.ConversationResponse;
import com.blog.dto.chat.MessageResponse;
import com.blog.dto.chat.SendMessageRequest;
import com.blog.security.SecurityUtils;
import com.blog.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "私信", description = "站内私信：会话列表 / 历史消息 / 发送 / 未读数（实时推送走 /api/v1/ws/chat）")
@RestController
@RequestMapping("/api/v1")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "我的会话列表", description = "需登录；按最后消息时间倒序，含对方用户、最后一条预览与我的未读数")
    @GetMapping("/conversations")
    public ApiResponse<PageResult<ConversationResponse>> conversations(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.ok(chatService.listConversations(page, pageSize, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "会话历史消息", description = "需登录；仅会话双方可看；按时间正序返回；拉取即把对方消息置为已读并清零未读数")
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<PageResult<MessageResponse>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.ok(chatService.listMessages(id, page, pageSize, SecurityUtils.currentUserId()));
    }

    @Operation(summary = "发送私信", description = "需登录；toUserId 为对方用户 ID，会话不存在自动创建；对方在线时经 WebSocket 实时收到 {type:\"message\"}")
    @PostMapping("/messages")
    public ApiResponse<MessageResponse> send(@Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.ok(chatService.send(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "我的未读消息总数", description = "需登录；所有会话未读数求和，用于全局消息角标")
    @GetMapping("/messages/unread-total")
    public ApiResponse<Long> unreadTotal() {
        return ApiResponse.ok(chatService.totalUnread(SecurityUtils.currentUserId()));
    }
}
