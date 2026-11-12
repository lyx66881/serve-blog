package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理端更新反馈处理状态请求：pending / processed。
 */
public record AdminFeedbackStatusRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
