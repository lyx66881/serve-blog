package com.blog.dto.feedback;

import jakarta.validation.constraints.Size;

/**
 * 提交意见反馈：content 与 imageId 至少其一（服务层校验，二者皆空报 40006）。
 * 图片须先经 POST /upload/image?type=feedback 上传，imageId 为本人上传记录 ID；
 * imageWidth/imageHeight 可选，供前端占位。
 */
public record SubmitFeedbackRequest(
        @Size(max = 1000, message = "反馈内容最长 1000 字符")
        String content,

        Long imageId,

        Integer imageWidth,

        Integer imageHeight
) {
}
