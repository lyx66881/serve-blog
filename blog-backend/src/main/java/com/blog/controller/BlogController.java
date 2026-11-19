package com.blog.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogResponse;
import com.blog.dto.blog.BlogSaveRequest;
import com.blog.dto.blog.BlogSearchResponse;
import com.blog.dto.blog.BlogSummaryResponse;
import com.blog.security.SecurityUtils;
import com.blog.service.BlogService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "博客", description = "博客 CRUD / 草稿 / 发布 / 下架")
@RestController
@RequestMapping("/api/v1/blogs")
public class BlogController {

    private final BlogService blogService;

    public BlogController(BlogService blogService) {
        this.blogService = blogService;
    }

    @Operation(summary = "公开博客列表", description = "公开接口，仅返回已发布博客；支持分类/标签/作者筛选与关键词搜索")
    @GetMapping
    public ApiResponse<PageResult<BlogSummaryResponse>> list(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(blogService.listPublic(page, pageSize, keyword, categoryId, tagId, authorId, sort));
    }

    @Operation(summary = "站内搜索", description = "按标题或作者（用户名/昵称）模糊搜索已发布公开文章；附带命中的作者列表")
    @GetMapping("/search")
    public ApiResponse<BlogSearchResponse> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(blogService.search(keyword, page, pageSize));
    }

    @Operation(summary = "我的博客列表", description = "需登录；status 可选 draft / published / offline，不传返回全部")
    @GetMapping("/my")
    public ApiResponse<PageResult<BlogSummaryResponse>> my(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(blogService.listMine(status, page, pageSize));
    }

    @Operation(summary = "博客详情", description = "公开接口：已发布博客所有人可见；草稿 / 下架博客仅作者可见")
    @GetMapping("/{id}")
    public ApiResponse<BlogResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(blogService.getDetail(id, SecurityUtils.currentUserIdOrNull()));
    }

    @Operation(summary = "创建博客", description = "status=draft 保存草稿（默认），status=published 直接发布")
    @PostMapping
    public ApiResponse<BlogResponse> create(@RequestBody BlogSaveRequest request) {
        return ApiResponse.ok(blogService.create(SecurityUtils.currentUserId(), request));
    }

    @Operation(summary = "编辑博客", description = "仅作者可编辑；字段可选，仅非空字段生效")
    @PutMapping("/{id}")
    public ApiResponse<BlogResponse> update(@PathVariable Long id, @RequestBody BlogSaveRequest request) {
        return ApiResponse.ok(blogService.update(SecurityUtils.currentUserId(), id, request));
    }

    @Operation(summary = "删除博客", description = "仅作者可删除（逻辑删除）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        blogService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok();
    }

    @Operation(summary = "发布博客", description = "仅作者可发布；草稿或下架 -> 已发布")
    @PostMapping("/{id}/publish")
    public ApiResponse<BlogResponse> publish(@PathVariable Long id) {
        return ApiResponse.ok(blogService.publish(SecurityUtils.currentUserId(), id));
    }

    @Operation(summary = "下架博客", description = "仅作者可下架；已发布 -> 下架（不再公开可见）")
    @PostMapping("/{id}/unpublish")
    public ApiResponse<BlogResponse> unpublish(@PathVariable Long id) {
        return ApiResponse.ok(blogService.unpublish(SecurityUtils.currentUserId(), id));
    }
}
