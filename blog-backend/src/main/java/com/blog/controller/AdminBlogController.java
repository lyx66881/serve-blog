package com.blog.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.admin.AdminBlogStatusRequest;
import com.blog.dto.blog.BlogResponse;
import com.blog.dto.blog.BlogSummaryResponse;
import com.blog.service.BlogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端博客管理接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-博客", description = "全站博客管理（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/blogs")
public class AdminBlogController {

    private final BlogService blogService;

    public AdminBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @Operation(summary = "博客列表", description = "全部作者与状态；keyword 匹配标题或作者用户名/昵称")
    @GetMapping
    public ApiResponse<PageResult<BlogSummaryResponse>> list(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId) {
        return ApiResponse.ok(blogService.adminList(page, pageSize, keyword, status, categoryId, authorId));
    }

    @Operation(summary = "博客详情", description = "查看任意博客（含草稿/下架），不增加浏览量")
    @GetMapping("/{id}")
    public ApiResponse<BlogResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(blogService.adminDetail(id));
    }

    @Operation(summary = "修改博客状态", description = "status：published-上架 / offline-下架 / draft-转草稿")
    @PutMapping("/{id}/status")
    public ApiResponse<BlogResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminBlogStatusRequest request) {
        return ApiResponse.ok(blogService.adminUpdateStatus(id, request.status()));
    }

    @Operation(summary = "删除博客", description = "逻辑删除任意作者的博客")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        blogService.adminDelete(id);
        return ApiResponse.ok();
    }
}
