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
import com.blog.dto.admin.AdminUserResetPasswordRequest;
import com.blog.dto.admin.AdminUserResponse;
import com.blog.dto.admin.AdminUserRoleRequest;
import com.blog.dto.admin.AdminUserStatusRequest;
import com.blog.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "用户管理", description = "管理员：全站用户管理（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "全站用户列表", description = "管理员接口；keyword 可匹配用户名 / 昵称 / 邮箱，附博客数")
    @GetMapping
    public ApiResponse<PageResult<AdminUserResponse>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(userService.adminPage(keyword, page, pageSize));
    }

    @Operation(summary = "封禁 / 解封用户", description = "管理员接口；status = 0-正常 / 1-封禁，封禁即时生效；不能操作自己")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminUserResponse> setStatus(@PathVariable Long id,
                                                    @Valid @RequestBody AdminUserStatusRequest request) {
        return ApiResponse.ok(userService.adminSetStatus(id, request.status()));
    }

    @Operation(summary = "调整用户角色", description = "管理员接口；role = user-普通用户 / admin-管理员，即时生效；不能操作自己")
    @PutMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> setRole(@PathVariable Long id,
                                                  @Valid @RequestBody AdminUserRoleRequest request) {
        return ApiResponse.ok(userService.adminSetRole(id, request.role()));
    }

    @Operation(summary = "重置用户密码", description = "管理员接口；newPassword 长度 6-64，BCrypt 加密存储")
    @PutMapping("/{id}/password")
    public ApiResponse<Void> resetPassword(@PathVariable Long id,
                                           @Valid @RequestBody AdminUserResetPasswordRequest request) {
        userService.adminResetPassword(id, request.newPassword());
        return ApiResponse.ok(null);
    }
}
