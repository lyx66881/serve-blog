package com.blog.dto.upload;

/**
 * 图片上传响应。
 */
public record UploadResponse(
        Long id,
        String url,
        String fileName,
        long size,
        String contentType,
        String type
) {
}
