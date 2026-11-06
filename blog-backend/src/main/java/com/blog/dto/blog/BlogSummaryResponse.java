package com.blog.dto.blog;

import com.blog.dto.tag.TagResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客列表项响应（不含正文，避免列表接口返回大字段）。
 */
public record BlogSummaryResponse(
        Long id,
        BlogAuthorResponse author,
        String title,
        String summary,
        String coverImage,
        Long categoryId,
        String categoryName,
        List<TagResponse> tags,
        String status,
        Boolean isPublic,
        Long viewCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
