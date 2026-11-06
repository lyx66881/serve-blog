package com.blog.dto.blog;

import java.time.LocalDateTime;

/**
 * 博客作者简要信息（公开）。
 */
public record BlogAuthorResponse(
        Long id,
        String username,
        String nickname,
        String avatar
) {
}
