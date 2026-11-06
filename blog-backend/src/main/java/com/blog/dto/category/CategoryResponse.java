package com.blog.dto.category;

/**
 * 分类响应。
 */
public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}
