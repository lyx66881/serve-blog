package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理端博客状态变更请求：draft-草稿 / published-发布 / offline-下架。
 */
public record AdminBlogStatusRequest(
        @NotBlank(message = "status 不能为空")
        String status
) {
}
