package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端重置用户密码请求。
 */
public record AdminUserResetPasswordRequest(
        @NotBlank(message = "newPassword 不能为空")
        @Size(min = 6, max = 64, message = "新密码长度需在 6-64 位之间")
        String newPassword
) {
}
