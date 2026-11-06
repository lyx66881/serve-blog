package com.blog.dto.user;

import java.time.LocalDateTime;

/**
 * 用户公开主页信息（不含 email）。
 */
public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatar,
        String bio,
        LocalDateTime createdAt,
        long blogCount
) {
}
