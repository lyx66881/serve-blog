package com.blog.dto.chat;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 发送私信请求：toUserId 为对方用户 ID，会话不存在时自动创建。
 * <p>
 * 两种消息类型：
 * - 文本消息：imageId 为空，content 必填（1-2000 字符）；
 * - 图片消息：imageId 为经 POST /upload/image?bizType=chat 上传后的文件 ID，
 *   content 可选（作为图片配文，0-2000 字符），imageWidth/imageHeight 可选（前端占位用）。
 */
public record SendMessageRequest(
        @NotNull(message = "toUserId 不能为空")
        Long toUserId,

        @Size(max = 2000, message = "消息内容最长 2000 字符")
        String content,

        Long imageId,

        Integer imageWidth,

        Integer imageHeight
) {
}
