package com.blog.dto.admin;

import java.time.LocalDateTime;

/**
 * 管理端反馈列表项（扁平结构，便于后台表格直接渲染）。
 * status = pending-待处理 / processed-已处理 / ignored-已忽略；contact 预留字段，当前恒为 null。
 */
public record AdminFeedbackResponse(
        Long id,
        Long userId,
        String username,
        String nickname,
        String avatar,
        String contact,
        String content,
        String imageUrl,
        Integer imageWidth,
        Integer imageHeight,
        String status,
        LocalDateTime createdAt
) {
}
