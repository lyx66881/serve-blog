package com.blog.dto.feedback;

import java.time.LocalDateTime;

/**
 * 意见反馈项（管理端列表）。
 * 提交用户可能已注销或匿名，username / nickname / avatar 可为空。
 */
public record FeedbackResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        String avatar,
        String contact,
        String content,
        String status,
        LocalDateTime createdAt
) {
}
