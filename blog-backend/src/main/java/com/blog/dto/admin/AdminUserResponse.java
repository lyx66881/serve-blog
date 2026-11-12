package com.blog.dto.admin;

import java.time.LocalDateTime;

/**
 * 管理端用户列表项。
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
