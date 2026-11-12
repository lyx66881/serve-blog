package com.blog.dto.admin;

import java.util.List;

/**
 * 数据看板统计：总量指标 + 近 14 天发文 / 新增用户趋势。
 */
public record AdminStatsResponse(
        long totalUsers,
        long totalBlogs,
        long publishedBlogs,
        long draftBlogs,
        long offlineBlogs,
        long totalViews,
        long totalCategories,
        long totalTags,
        long totalFeedbacks,
        long pendingFeedbacks,
        List<TrendPoint> trend
) {
}
