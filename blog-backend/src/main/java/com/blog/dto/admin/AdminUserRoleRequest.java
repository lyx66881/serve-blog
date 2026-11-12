package com.blog.dto.admin;

import jakarta.validation.constraints.Pattern;

/**
 * 管理端用户角色请求：user-普通用户 / admin-管理员。
 */
public record AdminUserRoleRequest(
        @Pattern(regexp = "user|admin", message = "role 仅支持 user / admin")
        String role
) {
}
