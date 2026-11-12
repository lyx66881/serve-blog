package com.blog.security;

/**
 * 认证通过后的用户主体，存放在 SecurityContext 中。
 * role 为 users.role 的值（user / admin）。
 */
public record UserPrincipal(Long id, String username, String role) {

    public boolean isAdmin() {
        return "admin".equals(role);
    }
}
