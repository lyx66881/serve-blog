package com.blog.controller;

import java.util.List;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.admin.AdminCategoryUpdateRequest;
import com.blog.dto.category.CategoryCreateRequest;
import com.blog.dto.category.CategoryResponse;
import com.blog.service.CategoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端分类管理接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-分类", description = "分类增删改（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Operation(summary = "创建分类", description = "分类名唯一")
    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryCreateRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody AdminCategoryUpdateRequest request) {
        return ApiResponse.ok(categoryService.adminUpdate(id, request));
    }

    @Operation(summary = "删除分类", description = "逻辑删除；引用该分类的博客列表将不再显示分类名")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.adminDelete(id);
        return ApiResponse.ok();
    }
}
