package com.blog.dto.user;

import java.time.LocalDateTime;

/**
 * 用户公开主页信息（不含 email）。
 * followedByMe=访问者是否已关注该用户；followsMe=该用户是否关注了访问者（未登录时均为 null）。
 */
public record UserProfileResponse(
        Long id,
        String username,
        String nickname,
        String avatar,
        String bio,
        LocalDateTime createdAt,
        long blogCount,
        long followersCount,
        long followingCount,
        Boolean followedByMe,
        Boolean followsMe,
        String dmPolicy
) {
}
