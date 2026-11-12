package com.blog.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 管理端封禁 / 解封用户请求。
 */
public record AdminUserStatusRequest(
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态仅支持 0-正常 1-封禁")
        @Max(value = 1, message = "状态仅支持 0-正常 1-封禁")
        Integer status
) {
}
