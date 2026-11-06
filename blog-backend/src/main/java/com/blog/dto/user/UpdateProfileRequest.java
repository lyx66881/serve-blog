package com.blog.dto.user;

import jakarta.validation.constraints.Size;

/**
 * 修改个人资料请求。所有字段可选，仅非空字段生效；用户名 / 邮箱不可修改。
 */
public record UpdateProfileRequest(
        @Size(max = 50, message = "昵称最长 50 字符")
        String nickname,

        @Size(max = 500, message = "简介最长 500 字符")
        String bio,

        @Size(max = 500, message = "头像 URL 最长 500 字符")
        String avatar
) {
}
