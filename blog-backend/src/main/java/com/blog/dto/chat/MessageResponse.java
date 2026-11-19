package com.blog.dto.chat;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 私信消息响应。read=true 表示对方已读；自己发的消息恒为 read=true（对自己无未读概念）。
 * msgType=image 时 imageUrl 必有值，content 可为图片配文（可能为空字符串或 null）。
 */
public record MessageResponse(
        Long id,
        Long conversationId,
        BlogAuthorResponse sender,
        String content,
        String msgType,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight,
        boolean read,
        LocalDateTime createdAt
) {
}
