package com.blog.dto.feedback;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 提交意见反馈请求：匿名可提交（留联系方式），登录用户可自动关联账号。
 */
public record FeedbackCreateRequest(
        @Size(max = 100, message = "联系方式最长 100 字符")
        String contact,

        @NotBlank(message = "反馈内容不能为空")
        @Size(max = 1000, message = "反馈内容最长 1000 字符")
        String content
) {
}
