package com.blog.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 忘记密码请求：account 支持用户名或邮箱。
 */
public record ForgotPasswordRequest(
        @NotBlank(message = "用户名或邮箱不能为空")
        @Size(max = 100, message = "用户名或邮箱最长 100 字符")
        String account
) {
}
