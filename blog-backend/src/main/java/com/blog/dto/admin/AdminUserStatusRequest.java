package com.blog.dto.admin;

import jakarta.validation.constraints.NotNull;

/**
 * 管理端用户封禁状态请求：0-正常 / 1-封禁。
 */
public record AdminUserStatusRequest(
        @NotNull(message = "status 不能为空")
        Integer status
) {
}
