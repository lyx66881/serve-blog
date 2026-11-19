package com.blog.dto.admin;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项：status = 0-正常 / 1-已封禁，附博客数。
 */
public record AdminUserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        String avatar,
        String role,
        Integer status,
        Long blogCount,
        LocalDateTime createdAt
) {
}
