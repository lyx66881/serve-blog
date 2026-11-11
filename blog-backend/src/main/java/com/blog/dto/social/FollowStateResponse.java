package com.blog.dto.social;

/**
 * 关注/取关结果：操作后的关注状态。
 */
public record FollowStateResponse(
        boolean following
) {
}
