package com.blog.dto.notification;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 通知列表项：type = follow-新关注（actor 即触发者）。
 */
public record NotificationResponse(
        Long id,
        String type,
        BlogAuthorResponse actor,
        Boolean isRead,
        LocalDateTime createdAt
) {
}
