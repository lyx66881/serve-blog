package com.blog.dto.chat;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 私信消息响应。read=true 表示对方已读；自己发的消息恒为 read=true（对自己无未读概念）。
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        BlogAuthorResponse sender,
        String content,
        boolean read,
        LocalDateTime createdAt
) {
}
