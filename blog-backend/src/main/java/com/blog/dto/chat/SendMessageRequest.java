package com.blog.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送私信请求：toUserId 为对方用户 ID，会话不存在时自动创建。
 */
public record SendMessageRequest(
        @NotNull(message = "toUserId 不能为空")
        Long toUserId,

        @NotBlank(message = "消息内容不能为空")
        @Size(max = 2000, message = "消息内容最长 2000 字符")
        String content
) {
}
