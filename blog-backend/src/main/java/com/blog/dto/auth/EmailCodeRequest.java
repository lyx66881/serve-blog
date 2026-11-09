package com.blog.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 发送邮箱验证码请求。
 * scene=register：邮箱必须未被注册；scene=reset：邮箱必须是已注册用户的邮箱；
 * scene=change：需登录，邮箱必须与当前用户绑定邮箱一致。
 */
public record EmailCodeRequest(
        @NotBlank(message = "邮箱不能为空")
        @Email(message = "邮箱格式不正确")
        @Size(max = 100, message = "邮箱最长 100 字符")
        String email,

        @NotBlank(message = "场景不能为空")
        @Pattern(regexp = "^(register|reset|change)$", message = "场景须为 register / reset / change")
        String scene
) {
}
