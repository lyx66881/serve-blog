package com.blog.dto.admin;

import java.util.List;

/**
 * 管理端数据看板统计响应。
 * <pre>
 * {
 *   "totalUsers": 100, "totalBlogs": 300, "publishedBlogs": 200, "draftBlogs": 50, "offlineBlogs": 50,
 *   "totalViews": 12000, "totalCategories": 10, "totalTags": 40,
 *   "totalFeedbacks": 20, "pendingFeedbacks": 5,
 *   "trend": [ { "date": "2026-11-01", "blogsPublished": 3, "newUsers": 2 }, ... ]  // 近 14 天
 * }
 * </pre>
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

    /**
     * 单日趋势点：date 格式 yyyy-MM-dd。
     */
    public record TrendPoint(String date, long blogsPublished, long newUsers) {
    }
}
