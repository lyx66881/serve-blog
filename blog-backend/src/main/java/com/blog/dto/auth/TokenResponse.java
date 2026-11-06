package com.blog.dto.auth;

import com.blog.dto.user.UserResponse;

/**
 * 登录 / 注册响应：JWT 令牌 + 用户信息。
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
}
