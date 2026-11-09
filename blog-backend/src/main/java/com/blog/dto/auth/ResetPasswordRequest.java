package com.blog.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 重置密码请求：邮箱 + 邮箱验证码 + 新密码（scene=reset 的验证码）。
 */
public record ResetPasswordRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱最长 100 字符")
        String email,

        @NotBlank(message = "验证码不能为空")
        @Pattern(regexp = "^\\d{6}$", message = "验证码须为 6 位数字")
        String code,

        @NotBlank(message = "新密码不能为空")
        @Size(min = 6, max = 32, message = "密码长度须为 6-32 位")
        String newPassword
) {
}
