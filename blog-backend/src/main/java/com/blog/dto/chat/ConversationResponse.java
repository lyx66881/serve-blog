package com.blog.dto.chat;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 会话列表项响应：otherUser 为对方用户，lastMessage 为最后一条消息预览，unreadCount 为我的未读数。
 */
public record ConversationResponse(
        Long id,
        BlogAuthorResponse otherUser,
        LastMessage lastMessage,
        Long unreadCount,
        LocalDateTime updatedAt
) {

    public record LastMessage(
            Long senderId,
            String content,
            LocalDateTime createdAt
    ) {
    }
}
