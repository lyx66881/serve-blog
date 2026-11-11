package com.blog.dto.social;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 关注/粉丝列表项：用户卡片 + 关注时间 + 与当前登录用户的关系标记。
 * followedByMe=我是否关注了该项用户；followsMe=该项用户是否关注了我（可用于「互关」标记）。
 * 未登录时两个标记均为 null。
 */
public record FollowUserResponse(
        BlogAuthorResponse user,
        LocalDateTime followedAt,
        Boolean followedByMe,
        Boolean followsMe
) {
}
