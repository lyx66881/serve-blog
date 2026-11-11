package com.blog.dto.feedback;

import java.time.LocalDateTime;

import com.blog.dto.blog.BlogAuthorResponse;

/**
 * 意见反馈响应：status = pending-待处理 / processed-已处理 / ignored-已忽略。
 */
public record FeedbackResponse(
        Long id,
        BlogAuthorResponse user,
        String content,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight,
        String status,
        LocalDateTime createdAt
) {
}
