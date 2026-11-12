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

@Tag(name = "标签管理", description = "管理员：标签维护（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/tags")
public class AdminTagController {

    private final TagService tagService;

    public AdminTagController(TagService tagService) {
        this.tagService = tagService;
    }

    @Operation(summary = "全量标签列表", description = "管理员接口；按名称排序，附关联博客数")
    @GetMapping
    public ApiResponse<List<AdminTagResponse>> list() {
        return ApiResponse.ok(tagService.adminList());
    }

    @Operation(summary = "重命名标签", description = "管理员接口；标签名全局唯一，≤30 字符")
    @PutMapping("/{id}")
    public ApiResponse<TagResponse> rename(@PathVariable Long id,
                                           @Valid @RequestBody AdminTagRenameRequest request) {
        return ApiResponse.ok(tagService.adminRename(id, request.name()));
    }

    @Operation(summary = "删除标签", description = "管理员接口；同时移除该标签与博客的关联")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tagService.adminDelete(id);
        return ApiResponse.ok(null);
    }
}
