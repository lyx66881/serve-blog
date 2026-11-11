package com.blog.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 发表评论 / 回复请求。
 * <p>
 * content：1-1000 字；parentId：回复时的顶级评论 ID（不传为发表顶级评论）；
 * replyToUserId：被回复人（回复某条回复时传该回复作者，不传默认为父评论作者）。
 */
public record CommentCreateRequest(
        @NotBlank(message = "评论内容不能为空")
        @Size(max = 1000, message = "评论内容最长 1000 字符")
        String content,

        Long parentId,

        Long replyToUserId
) {
}
