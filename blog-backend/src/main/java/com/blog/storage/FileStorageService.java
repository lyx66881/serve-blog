package com.blog.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 统一文件存储接口。
 * <p>
 * 当前实现为服务器本地磁盘存储（{@link LocalFileStorageService}）；
 * 未来切换 MinIO / OSS 等对象存储时，新增实现类并替换注册即可，业务层不感知。
 */
public interface FileStorageService {

    /**
     * 存储上传文件，返回存储结果（含可访问 URL）。
     *
     * @param file    上传文件
     * @param bizType 业务类型：avatar / cover / article
     */
    StoredFile store(MultipartFile file, String bizType);

    /**
     * 删除文件（按相对路径）。
     */
    void delete(String relativePath);
}
