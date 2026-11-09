package com.blog.dto.auth;

/**
 * 发送邮箱验证码响应。
 * sentTo：脱敏后的收件邮箱；devCode：仅模拟模式下返回（真实邮件发送后不返回）。
 */
public record EmailCodeResponse(
        boolean sent,
        String sentTo,
        String devCode,
        long expiresInSeconds,
        long resendIntervalSeconds
) {
}
