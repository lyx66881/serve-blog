package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.admin.AdminResetPasswordRequest;
import com.blog.dto.admin.AdminUserResponse;
import com.blog.dto.admin.AdminUserRoleRequest;
import com.blog.dto.admin.AdminUserStatusRequest;
import com.blog.security.SecurityUtils;
import com.blog.service.AdminUserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端用户管理接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-用户", description = "用户管理（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @Operation(summary = "用户列表", description = "keyword 匹配用户名/昵称/邮箱；可按 role、status 筛选；含博客数")
    @GetMapping
    public ApiResponse<PageResult<AdminUserResponse>> list(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Integer status) {
        return ApiResponse.ok(adminUserService.list(page, pageSize, keyword, role, status));
    }

    @Operation(summary = "用户详情")
    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(adminUserService.detail(id));
    }

    @Operation(summary = "封禁 / 解封用户", description = "status：0-正常 1-封禁；封禁后无法登录；不能操作自己")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.ok(adminUserService.updateStatus(SecurityUtils.currentUserId(), id, request.status()));
    }

    @Operation(summary = "调整用户角色", description = "role：user / admin；不能修改自己")
    @PutMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> updateRole(@PathVariable Long id, @Valid @RequestBody AdminUserRoleRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(SecurityUtils.currentUserId(), id, request.role()));
    }

    @Operation(summary = "重置用户密码", description = "管理员直接设置新密码（6-32 位）")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id, @Valid @RequestBody AdminResetPasswordRequest request) {
        adminUserService.resetPassword(id, request.newPassword());
        return ApiResponse.ok();
    }
}
