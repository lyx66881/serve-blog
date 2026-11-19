package com.blog.dto.social;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 更新私信策略请求。
 */
public record UpdateDmPolicyRequest(
        @NotBlank
        @Pattern(regexp = "everyone|mutual_follow", message = "私信策略仅支持 everyone 或 mutual_follow")
        String policy
) {
}
