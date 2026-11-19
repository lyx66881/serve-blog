package com.blog.security;

/**
 * 认证通过后的用户主体，存放在 SecurityContext 中。
 */
public record UserPrincipal(Long id, String username) {
}
