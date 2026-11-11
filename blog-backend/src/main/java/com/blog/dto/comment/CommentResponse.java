package com.blog.dto.comment;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 评论响应（公开列表与管理端通用）。
 * <p>
 * 顶级评论 replyCount 为其可见回复数；回复本身 replyCount 恒为 0。
 */
public record CommentResponse(
        Long id,
        Long blogId,
        Long parentId,
        BlogAuthorResponse author,
        String content,
        BlogAuthorResponse replyToUser,
        Long replyCount,
        String status,
        LocalDateTime createdAt
) {
}
