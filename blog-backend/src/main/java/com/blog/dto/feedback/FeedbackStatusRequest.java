package com.blog.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理端修改反馈状态请求。
 */
public record FeedbackStatusRequest(
        @NotBlank(message = "status 不能为空")
        @Pattern(regexp = "pending|processed|ignored", message = "status 仅支持 pending / processed / ignored")
        String status
) {
}
