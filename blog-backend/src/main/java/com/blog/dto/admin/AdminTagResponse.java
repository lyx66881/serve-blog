package com.blog.dto.admin;

/**
 * 管理端标签列表项：附关联博客数。
 */
public record AdminTagResponse(
        Long id,
        String name,
        Long blogCount
) {
}
