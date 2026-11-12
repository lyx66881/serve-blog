package com.blog.dto.blog;

import java.util.List;

/**
 * 站内搜索结果：按标题或作者模糊匹配已发布公开文章。
 * 分页字段与 PageResult 一致，另附命中的作者（供前端展示"找到这些作者"建议）。
 */
public record BlogSearchResponse(
        long total,
        long page,
        long pageSize,
        long pages,
        List<BlogAuthorResponse> matchedAuthors,
        List<BlogSummaryResponse> items
) {
}
