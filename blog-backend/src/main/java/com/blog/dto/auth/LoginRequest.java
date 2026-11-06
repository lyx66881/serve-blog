package com.blog.dto.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * 登录请求。username 字段支持用户名或邮箱。
 */
public record LoginRequest(
        @NotBlank(message = "用户名不能为空")
        String username,

        @NotBlank(message = "密码不能为空")
        String password
) {
}
