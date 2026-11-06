package com.blog.dto.blog;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.Size;

/**
 * 创建 / 编辑博客请求。
 * <p>
 * content 为 TipTap / ProseMirror JSON 对象（原样存储，服务端不修改结构）。
 * status 仅在创建时生效：draft（默认）/ published；编辑时通过
 * POST /{id}/publish 与 POST /{id}/unpublish 接口变更状态。
 */
public record BlogSaveRequest(
        @Size(max = 200, message = "标题最长 200 字符")
        String title,

        JsonNode content,

        String contentHtml,

        @Size(max = 500, message = "摘要最长 500 字符")
        String summary,

        @Size(max = 500, message = "封面 URL 最长 500 字符")
        String coverImage,

        Long categoryId,

        List<String> tags,

        String status
) {
}
