package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 管理端调整用户角色请求：user / admin。
 */
public record AdminUserRoleRequest(
        @NotBlank(message = "角色不能为空")
        @Pattern(regexp = "^(user|admin)$", message = "角色仅支持 user / admin")
        String role
) {
}
