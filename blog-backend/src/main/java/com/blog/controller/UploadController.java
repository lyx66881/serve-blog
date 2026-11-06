package com.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blog.common.ApiResponse;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.upload.UploadResponse;
import com.blog.service.UploadService;
import com.blog.storage.LocalFileStorageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "文件上传", description = "统一图片上传：头像 / 封面 / 正文图片")
@RestController
@RequestMapping("/api/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @Operation(summary = "上传图片", description = "multipart/form-data；type: avatar / cover / article（默认 article）")
    @PostMapping("/image")
    public ApiResponse<UploadResponse> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "article") String type) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        if (!LocalFileStorageService.BIZ_TYPES.contains(type)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "type 仅支持 avatar / cover / article");
        }
        return ApiResponse.ok(uploadService.uploadImage(file, type));
    }

    @Operation(summary = "删除已上传文件", description = "仅上传者本人可删除；文件被博客引用时删除会导致图片失效")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        uploadService.deleteFile(id);
        return ApiResponse.ok();
    }
}
