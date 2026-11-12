package com.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.admin.AdminTagRenameRequest;
import com.blog.dto.admin.AdminTagResponse;
import com.blog.dto.tag.TagResponse;
import com.blog.service.TagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端标签管理接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-标签", description = "标签重命名/删除（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "标签列表", description = "含每个标签关联的博客数")
    @GetMapping
    public ApiResponse<List<AdminTagResponse>> list() {
        return ApiResponse.ok(tagService.adminList());
    }

    @Operation(summary = "重命名标签", description = "标签名全局唯一，最长 30 字符")
    @PutMapping("/{id}")
    public ApiResponse<TagResponse> rename(@PathVariable Long id, @Valid @RequestBody AdminTagRenameRequest request) {
        return ApiResponse.ok(tagService.adminRename(id, request.name()));
    }

    @Operation(summary = "删除标签", description = "同时移除所有博客与该标签的关联")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.adminDelete(id);
        return ApiResponse.ok();
    }
}
