package com.blog.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.blog.dto.admin.AdminStatsResponse;
import com.blog.dto.admin.TrendPoint;
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
 * 数据看板统计：总量指标 + 近 14 天发文 / 新增用户趋势（缺失日期补 0）。
 */
@Service
public class AdminStatsService {

    private static final int TREND_DAYS = 14;

    private final BlogMapper blogMapper;
    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;
    private final FeedbackMapper feedbackMapper;

    public AdminStatsService(BlogMapper blogMapper, UserMapper userMapper, CategoryMapper categoryMapper,
                             TagMapper tagMapper, FeedbackMapper feedbackMapper) {
        this.blogMapper = blogMapper;
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
        this.feedbackMapper = feedbackMapper;
    }

    public AdminStatsResponse stats() {
        long totalUsers = userMapper.selectCount(null);
        long totalBlogs = blogMapper.selectCount(null);
        long published = blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getStatus, Blog.STATUS_PUBLISHED));
        long draft = blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getStatus, Blog.STATUS_DRAFT));
        long offline = blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getStatus, Blog.STATUS_OFFLINE));
        long totalViews = sumViewCount();
        long totalCategories = categoryMapper.selectCount(null);
        long totalTags = tagMapper.selectCount(null);
        long totalFeedbacks = feedbackMapper.selectCount(null);
        long pendingFeedbacks = feedbackMapper.selectCount(
                new LambdaQueryWrapper<Feedback>().eq(Feedback::getStatus, "pending"));

        return new AdminStatsResponse(totalUsers, totalBlogs, published, draft, offline,
                totalViews, totalCategories, totalTags, totalFeedbacks, pendingFeedbacks, trend());
    }

    private long sumViewCount() {
        QueryWrapper<Blog> qw = new QueryWrapper<Blog>().select("COALESCE(SUM(view_count), 0) AS s");
        List<Map<String, Object>> rows = blogMapper.selectMaps(qw);
        if (rows.isEmpty() || rows.get(0) == null || rows.get(0).get("s") == null) {
            return 0L;
        }
        return Long.parseLong(rows.get(0).get("s").toString());
    }

    /** 近 14 天（含今天）发文 / 新增用户趋势，缺失日期补 0。 */
    private List<TrendPoint> trend() {
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(TREND_DAYS - 1L);
        LocalDateTime startTime = start.atStartOfDay();

        Map<LocalDate, Long> publishedByDay = toDayCounts(blogMapper.selectMaps(
                new QueryWrapper<Blog>()
                        .select("DATE(published_at) AS d", "COUNT(*) AS c")
                        .isNotNull("published_at")
                        .ge("published_at", startTime)
                        .groupBy("d")));
        Map<LocalDate, Long> usersByDay = toDayCounts(userMapper.selectMaps(
                new QueryWrapper<User>()
                        .select("DATE(created_at) AS d", "COUNT(*) AS c")
                        .ge("created_at", startTime)
                        .groupBy("d")));

        List<TrendPoint> trend = new ArrayList<>(TREND_DAYS);
        for (LocalDate date = start; !date.isAfter(today); date = date.plusDays(1)) {
            trend.add(new TrendPoint(date.toString(),
                    publishedByDay.getOrDefault(date, 0L), usersByDay.getOrDefault(date, 0L)));
        }
        return trend;
    }

    /** 把 SELECT DATE(...) AS d, COUNT(*) AS c 的结果转为按天计数。 */
    private Map<LocalDate, Long> toDayCounts(List<Map<String, Object>> rows) {
        Map<LocalDate, Long> result = new HashMap<>();
        if (rows == null) {
            return result;
        }
        for (Map<String, Object> row : rows) {
            Object d = row.get("d");
            Object c = row.get("c");
            if (d != null && c != null) {
                result.put(LocalDate.parse(d.toString()), Long.parseLong(c.toString()));
            }
        }
        return result;
    }
}
