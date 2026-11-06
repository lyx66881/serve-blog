package com.blog.storage;

/**
 * 存储结果。
 *
 * @param relativePath 相对 base-dir 的路径，如 article/2026/11/uuid.png
 * @param url          前端可访问的 URL，如 /uploads/article/2026/11/uuid.png
 * @param originalName 原始文件名
 * @param storedName   存储文件名（随机生成）
 * @param size         文件大小（字节）
 * @param contentType  MIME 类型
 */
public record StoredFile(String relativePath, String url, String originalName,
                         String storedName, long size, String contentType) {
}
