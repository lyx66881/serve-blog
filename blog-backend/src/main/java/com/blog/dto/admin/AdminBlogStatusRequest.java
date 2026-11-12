package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;

/**
 * 管理端修改博客状态请求：published / offline（也可设回 draft）。
 */
public record AdminBlogStatusRequest(
        @NotBlank(message = "状态不能为空")
        String status
) {
}
