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

@Tag(name = "博客管理", description = "管理员：全站博客管理（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/blogs")
public class AdminBlogController {

    private final BlogService blogService;

    public AdminBlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @Operation(summary = "全站博客列表", description = "管理员接口；含草稿与下架，支持 keyword 标题模糊 / status / categoryId 筛选")
    @GetMapping
    public ApiResponse<PageResult<BlogSummaryResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(blogService.adminPage(keyword, status, categoryId, page, pageSize));
    }

    @Operation(summary = "博客详情", description = "管理员接口；任意状态可看，不累计浏览量")
    @GetMapping("/{id}")
    public ApiResponse<BlogResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(blogService.adminGetDetail(id));
    }

    @Operation(summary = "变更博客状态", description = "管理员接口；status = draft-转草稿 / published-发布 / offline-下架")
    @PutMapping("/{id}/status")
    public ApiResponse<BlogResponse> setStatus(@PathVariable Long id,
                                               @Valid @RequestBody AdminBlogStatusRequest request) {
        return ApiResponse.ok(blogService.adminSetStatus(id, request.status()));
    }

    @Operation(summary = "删除博客", description = "管理员接口；逻辑删除，不可恢复")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        blogService.adminDelete(id);
        return ApiResponse.ok(null);
    }
}
