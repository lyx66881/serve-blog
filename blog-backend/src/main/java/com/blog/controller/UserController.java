package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.blog.common.ApiResponse;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogSummaryResponse;
import com.blog.dto.user.ChangePasswordRequest;
import com.blog.dto.user.UpdateProfileRequest;
import com.blog.dto.user.UserProfileResponse;
import com.blog.dto.user.UserResponse;
import com.blog.security.SecurityUtils;
import com.blog.service.BlogService;
import com.blog.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "用户", description = "当前用户 / 用户主页")
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final BlogService blogService;

    public UserController(UserService userService, BlogService blogService) {
        this.userService = userService;
        this.blogService = blogService;
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/me")
    public ApiResponse<UserResponse> me() {
        return ApiResponse.ok(userService.getCurrentUser());
    }

    @Operation(summary = "修改个人资料", description = "昵称 / 简介 / 头像 URL，仅非空字段生效")
    @PutMapping("/me")
    public ApiResponse<UserResponse> updateMe(@Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.ok(userService.updateProfile(request));
    }

    @Operation(summary = "修改密码", description = "校验原密码后更新为新密码；成功后建议前端引导用户重新登录")
    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "上传头像", description = "multipart 上传，直接更新当前用户头像")
    @PostMapping("/me/avatar")
    public ApiResponse<UserResponse> uploadAvatar(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.PARAM_INVALID, "上传文件不能为空");
        }
        return ApiResponse.ok(userService.updateAvatar(file));
    }

    @Operation(summary = "用户公开主页", description = "公开接口：基本信息 + 已发布博客数")
    @GetMapping("/{id}")
    public ApiResponse<UserProfileResponse> profile(@PathVariable Long id) {
        return ApiResponse.ok(userService.getProfile(id));
    }

    @Operation(summary = "用户已发布博客", description = "公开接口：分页返回指定用户已发布博客")
    @GetMapping("/{id}/blogs")
    public ApiResponse<PageResult<BlogSummaryResponse>> userBlogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(blogService.listPublic(page, pageSize, null, null, null, id, null));
    }
}
