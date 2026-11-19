package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端标签重命名请求。
 */
public record AdminTagRenameRequest(
        @NotBlank(message = "标签名不能为空")
        @Size(max = 30, message = "标签名最长 30 字符")
        String name
) {
}
