package com.blog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.dto.admin.AdminStatsResponse;
import com.blog.entity.Blog;
import com.blog.entity.Category;
import com.blog.entity.Feedback;
import com.blog.entity.Tag;
import com.blog.entity.User;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.CategoryMapper;
import com.blog.mapper.FeedbackMapper;
import com.blog.mapper.TagMapper;
import com.blog.mapper.UserMapper;

/**
 * 管理端数据看板统计。
 */
@Service
public class AdminStatsService {

    private static final int TREND_DAYS = 14;

    private final UserMapper userMapper;
    private final BlogMapper blogMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final FeedbackMapper feedbackMapper;

    public AdminStatsService(UserMapper userMapper, BlogMapper blogMapper,
                             CategoryMapper categoryMapper, TagMapper tagMapper, FeedbackMapper feedbackMapper) {
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.feedbackMapper = feedbackMapper;
    }

    public AdminStatsResponse stats() {
        long totalUsers = userMapper.selectCount(null);
        long publishedBlogs = countBlogs(Blog.STATUS_PUBLISHED);
        long draftBlogs = countBlogs(Blog.STATUS_DRAFT);
        long offlineBlogs = countBlogs(Blog.STATUS_OFFLINE);
        long totalViews = sumViewCount();
        long totalCategories = categoryMapper.selectCount(null);
        long totalTags = tagMapper.selectCount(null);
        long totalFeedbacks = feedbackMapper.selectCount(null);
        long pendingFeedbacks = feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, Feedback.STATUS_PENDING));

        return new AdminStatsResponse(
                totalUsers, publishedBlogs + draftBlogs + offlineBlogs, publishedBlogs, draftBlogs, offlineBlogs,
                totalViews, totalCategories, totalTags, totalFeedbacks, pendingFeedbacks, buildTrend());
    }

    private long countBlogs(String status) {
        return blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getStatus, status));
    }

    private long sumViewCount() {
        QueryWrapper<Blog> wrapper = new QueryWrapper<>();
        wrapper.select("COALESCE(SUM(view_count), 0)").isNotNull("view_count");
        List<Object> objs = blogMapper.selectObjs(wrapper);
        if (objs.isEmpty() || objs.get(0) == null) {
            return 0;
        }
        return ((Number) objs.get(0)).longValue();
    }

    /**
     * 近 14 天趋势：每日发布博客数 / 新注册用户数（含今天，缺失日期补 0）。
     */
    private List<AdminStatsResponse.TrendPoint> buildTrend() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.minusDays(TREND_DAYS - 1).atStartOfDay();

        Map<LocalDate, Long> blogCounts = blogMapper.selectList(new LambdaQueryWrapper<Blog>()
                        .eq(Blog::getStatus, Blog.STATUS_PUBLISHED)
                        .ge(Blog::getPublishedAt, start)
                        .select(Blog::getPublishedAt))
                .stream()
                .filter(b -> b.getPublishedAt() != null)
                .collect(Collectors.groupingBy(b -> b.getPublishedAt().toLocalDate(), Collectors.counting()));

        Map<LocalDate, Long> userCounts = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .ge(User::getCreatedAt, start)
                        .select(User::getCreatedAt))
                .stream()
                .filter(u -> u.getCreatedAt() != null)
                .collect(Collectors.groupingBy(u -> u.getCreatedAt().toLocalDate(), Collectors.counting()));

        List<AdminStatsResponse.TrendPoint> trend = new ArrayList<>();
        for (int i = 0; i < TREND_DAYS; i++) {
            LocalDate day = today.minusDays(TREND_DAYS - 1 - i);
            trend.add(new AdminStatsResponse.TrendPoint(
                    day.toString(),
                    blogCounts.getOrDefault(day, 0L),
                    userCounts.getOrDefault(day, 0L)));
        }
        return trend;
    }
}
