package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端重置用户密码请求。
 */
public record AdminResetPasswordRequest(
        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
        String newPassword
) {
}
