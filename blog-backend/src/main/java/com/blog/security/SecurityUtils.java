package com.blog.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;

/**
 * 从 SecurityContext 获取当前登录用户。
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static UserPrincipal getPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }

    /**
     * 获取当前登录用户 ID，未登录抛出 UNAUTHORIZED。
     */
    public static Long currentUserId() {
        UserPrincipal principal = getPrincipal();
        if (principal == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        return principal.id();
    }

    /**
     * 获取当前登录用户 ID，未登录返回 null（用于公开接口的可选身份）。
     */
    public static Long currentUserIdOrNull() {
        UserPrincipal principal = getPrincipal();
        return principal == null ? null : principal.id();
    }

    /**
     * 当前用户是否为管理员。
     */
    public static boolean isAdmin() {
        UserPrincipal principal = getPrincipal();
        return principal != null && principal.isAdmin();
    }

    /**
     * 要求当前登录用户为管理员，否则抛出 FORBIDDEN。
     */
    public static void requireAdmin() {
        if (!isAdmin()) {
            throw new BizException(ErrorCode.FORBIDDEN, "需要管理员权限");
        }
    }
}
