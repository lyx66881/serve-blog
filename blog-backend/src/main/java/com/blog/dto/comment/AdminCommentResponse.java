package com.blog.dto.comment;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 管理端评论列表项：附文章标题，便于全站审核。
 */
public record AdminCommentResponse(
        Long id,
        Long blogId,
        String blogTitle,
        Long parentId,
        BlogAuthorResponse author,
        String content,
        String status,
        LocalDateTime createdAt
) {
}
