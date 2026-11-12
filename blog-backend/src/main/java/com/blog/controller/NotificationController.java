package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.notification.NotificationResponse;
import com.blog.dto.notification.UnreadCountResponse;
import com.blog.security.SecurityUtils;
import com.blog.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "站内通知", description = "通知列表 / 未读数 / 标记已读（需登录）")
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Operation(summary = "我的通知列表", description = "按时间倒序；onlyUnread=true 仅返回未读")
    @GetMapping
    public ApiResponse<PageResult<NotificationResponse>> page(
            @RequestParam(required = false) Boolean onlyUnread,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(notificationService.page(SecurityUtils.currentUserId(), onlyUnread, page, pageSize));
    }

    @Operation(summary = "未读通知数", description = "供角标轮询使用；新关注事件发生时也会通过 WS 推送 type=notify 实时提醒")
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount() {
        return ApiResponse.ok(notificationService.unreadCount(SecurityUtils.currentUserId()));
    }

    @Operation(summary = "标记单条已读")
    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id) {
        notificationService.markRead(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok(null);
    }

    @Operation(summary = "全部标记已读")
    @PutMapping("/read-all")
    public ApiResponse<Void> markAllRead() {
        notificationService.markAllRead(SecurityUtils.currentUserId());
        return ApiResponse.ok(null);
    }
}
