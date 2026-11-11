package com.blog.dto.social;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 我的拉黑列表项。
 */
public record BlockedUserResponse(
        BlogAuthorResponse user,
        LocalDateTime blockedAt
) {
}
