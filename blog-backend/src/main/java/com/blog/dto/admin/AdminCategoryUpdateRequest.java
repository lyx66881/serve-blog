package com.blog.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 管理端更新分类请求。
 */
public record AdminCategoryUpdateRequest(
        @NotBlank(message = "分类名不能为空")
        @Size(max = 50, message = "分类名最长 50 字符")
        String name,

        @Size(max = 200, message = "描述最长 200 字符")
        String description
) {
}
