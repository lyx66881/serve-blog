package com.blog.dto.user;

import java.time.LocalDateTime;

/**
 * 用户信息。
 * <p>
 * 作为「当前登录用户」返回时包含 email；作为公开信息返回时不包含 email。
 * null 字段不会输出（NON_NULL）。
 */
public record UserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String avatar,
        String bio,
        LocalDateTime createdAt
) {
}
