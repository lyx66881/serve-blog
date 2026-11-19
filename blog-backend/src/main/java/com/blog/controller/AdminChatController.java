package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.chat.ConversationResponse;
import com.blog.dto.chat.MessageResponse;
import com.blog.service.ChatService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "聊天记录管理", description = "管理员：查看用户会话与消息记录（只读，不影响已读状态）")
@RestController
@RequestMapping("/api/v1/admin/chats")
public class AdminChatController {

    private final ChatService chatService;

    public AdminChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @Operation(summary = "用户会话列表", description = "管理员接口；指定用户的全部会话，按最近消息时间倒序分页，含对方信息、最后一条消息与未读数")
    @GetMapping("/users/{userId}/conversations")
    public ApiResponse<PageResult<ConversationResponse>> userConversations(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "20") Long pageSize) {
        return ApiResponse.ok(chatService.adminListConversations(userId, page, pageSize));
    }

    @Operation(summary = "会话消息记录", description = "管理员接口；按时间正序分页，只读不标记已读；msgType=image 时含图片地址与尺寸")
    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<PageResult<MessageResponse>> messages(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "50") Long pageSize) {
        return ApiResponse.ok(chatService.adminListMessages(id, page, pageSize));
    }
}
