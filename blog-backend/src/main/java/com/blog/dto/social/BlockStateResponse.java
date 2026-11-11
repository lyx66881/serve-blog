package com.blog.dto.social;

/**
 * 拉黑/取消拉黑结果：操作后的拉黑状态。
 */
public record BlockStateResponse(
        boolean blocked
) {
}
