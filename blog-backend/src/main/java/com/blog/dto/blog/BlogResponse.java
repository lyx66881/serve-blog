package com.blog.dto.blog;

import com.blog.dto.tag.TagResponse;
import com.fasterxml.jackson.annotation.JsonRawValue;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 博客详情响应。
 * <p>
 * content 为 TipTap / ProseMirror JSON 对象，原样返回，保证编辑 / 阅读不丢失格式。
 */
public record BlogResponse(
        Long id,
        BlogAuthorResponse author,
        String title,
        String summary,
        String coverImage,
        Long categoryId,
        String categoryName,
        List<TagResponse> tags,
        String status,
        @JsonRawValue String content,
        String contentHtml,
        String contentVersion,
        Long viewCount,
        LocalDateTime publishedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
