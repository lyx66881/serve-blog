package com.blog.dto.admin;

/**
 * 管理端标签列表项（含关联博客数）。
 */
public record AdminTagResponse(
        Long id,
        String name,
        long blogCount
) {
}
