package com.blog.dto.social;

/**
 * 关注/粉丝统计。
 */
public record FollowStatsResponse(
        long followingCount,
        long followerCount
) {
}
