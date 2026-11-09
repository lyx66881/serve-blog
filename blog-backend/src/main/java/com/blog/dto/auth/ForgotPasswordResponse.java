package com.blog.dto.auth;

/**
 * 忘记密码响应。
 *
 * @param sent                 是否已发送验证码
 * @param devCode              验证码（仅未接入邮件服务且 expose-code=true 时返回，模拟发送；接入 SMTP 后不再输出）
 * @param expiresInSeconds     验证码有效期（秒）
 * @param resendIntervalSeconds 可再次发送的间隔（秒）
 */
public record ForgotPasswordResponse(
        boolean sent,
        String devCode,
        long expiresInSeconds,
        long resendIntervalSeconds
) {
}
