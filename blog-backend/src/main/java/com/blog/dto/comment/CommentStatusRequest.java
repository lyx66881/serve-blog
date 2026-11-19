package com.blog.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理端修改评论状态请求：visible-恢复显示 / hidden-隐藏。
 */
public record CommentStatusRequest(
        @NotBlank(message = "status 不能为空")
        @Pattern(regexp = "visible|hidden", message = "status 仅支持 visible / hidden")
        String status
) {
}
