package com.blog.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.upload.UploadResponse;
import com.blog.entity.UploadedFile;
import com.blog.mapper.UploadedFileMapper;
import com.blog.security.SecurityUtils;
import com.blog.storage.FileStorageService;
import com.blog.storage.StoredFile;

/**
 * 图片上传：统一入口，负责存储 + 记录 uploaded_files。
 */
@Service
public class UploadService {

    private final FileStorageService fileStorageService;
    private final UploadedFileMapper uploadedFileMapper;

    public UploadService(FileStorageService fileStorageService, UploadedFileMapper uploadedFileMapper) {
        this.fileStorageService = fileStorageService;
        this.uploadedFileMapper = uploadedFileMapper;
    }

    @Transactional
    public UploadResponse uploadImage(MultipartFile file, String bizType) {
        Long userId = SecurityUtils.currentUserId();
        StoredFile stored = fileStorageService.store(file, bizType);

        UploadedFile record = new UploadedFile();
        record.setUploaderId(userId);
        record.setBizType(bizType);
        record.setOriginalName(truncate(stored.originalName(), 255));
        record.setStoredName(stored.storedName());
        record.setRelativePath(stored.relativePath());
        record.setUrl(stored.url());
        record.setContentType(stored.contentType());
        record.setFileSize(stored.size());
        uploadedFileMapper.insert(record);

        return new UploadResponse(record.getId(), stored.url(), stored.originalName(),
                stored.size(), stored.contentType(), bizType);
    }

    /**
     * 删除自己上传的文件：移除磁盘文件并删除记录。
     * 注意：若文件已被博客引用，删除后对应图片将无法访问。
     */
    @Transactional
    public void deleteFile(Long fileId) {
        Long userId = SecurityUtils.currentUserId();
        UploadedFile record = uploadedFileMapper.selectById(fileId);
        if (record == null) {
            throw new BizException(ErrorCode.FILE_NOT_FOUND);
        }
        if (!record.getUploaderId().equals(userId)) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        fileStorageService.delete(record.getRelativePath());
        uploadedFileMapper.deleteById(fileId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
