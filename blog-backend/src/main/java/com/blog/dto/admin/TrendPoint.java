package com.blog.dto.admin;

/**
 * 数据看板趋势点（按天聚合）。
 */
public record TrendPoint(
        String date,
        long blogsPublished,
        long newUsers
) {
}
