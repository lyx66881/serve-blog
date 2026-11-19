package com.blog.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.notification.NotificationResponse;
import com.blog.dto.notification.UnreadCountResponse;
import com.blog.entity.Notification;
import com.blog.entity.User;
import com.blog.mapper.NotificationMapper;
import com.blog.mapper.UserMapper;
import com.blog.websocket.ChatWebSocketHandler;

/**
 * 站内通知业务：目前覆盖「新关注」事件；未读数供轮询 / WS 推送使用。
 */
@Service
public class NotificationService {

    private static final int MAX_PAGE_SIZE = 50;

    private final NotificationMapper notificationMapper;
    private final UserMapper userMapper;
    private final ChatWebSocketHandler chatWebSocketHandler;

    public NotificationService(NotificationMapper notificationMapper, UserMapper userMapper,
                               ChatWebSocketHandler chatWebSocketHandler) {
        this.notificationMapper = notificationMapper;
        this.userMapper = userMapper;
        this.chatWebSocketHandler = chatWebSocketHandler;
    }

    /**
     * 关注事件产生通知：同一人存在未读关注通知时不重复生成（防反复取关/关注刷通知）。
     * 成功后向接收人在线连接推送实时提示；离线则静默（上线后拉取）。
     */
    public void onFollow(Long actorId, Long targetId) {
        Long existed = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, targetId)
                .eq(Notification::getActorId, actorId)
                .eq(Notification::getType, Notification.TYPE_FOLLOW)
                .eq(Notification::getIsRead, 0));
        if (existed != null && existed > 0) {
            return;
        }
        Notification ntf = new Notification();
        ntf.setUserId(targetId);
        ntf.setType(Notification.TYPE_FOLLOW);
        ntf.setActorId(actorId);
        ntf.setIsRead(0);
        notificationMapper.insert(ntf);

        pushTo(targetId, "follow");
    }

    /**
     * 通知列表：默认全部，onlyUnread=true 仅未读；按时间倒序。
     */
    public PageResult<NotificationResponse> page(Long userId, Boolean onlyUnread, Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 10 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId);
        if (Boolean.TRUE.equals(onlyUnread)) {
            wrapper.eq(Notification::getIsRead, 0);
        }
        wrapper.orderByDesc(Notification::getId);

        Page<Notification> result = notificationMapper.selectPage(new Page<>(p, s), wrapper);
        Map<Long, User> actors = loadActors(result.getRecords());
        return PageResult.of(result, ntf -> toResponse(ntf, actors));
    }

    /** 未读通知数。 */
    public UnreadCountResponse unreadCount(Long userId) {
        Long count = notificationMapper.selectCount(new LambdaQueryWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0));
        return new UnreadCountResponse(count == null ? 0 : count);
    }

    /** 标记单条已读（仅本人）。 */
    public void markRead(Long userId, Long id) {
        Notification ntf = notificationMapper.selectById(id);
        if (ntf == null || !userId.equals(ntf.getUserId())) {
            throw new BizException(ErrorCode.NOT_FOUND, "通知不存在");
        }
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getId, id)
                .eq(Notification::getUserId, userId)
                .set(Notification::getIsRead, 1));
    }

    /** 全部标记已读。 */
    public void markAllRead(Long userId) {
        notificationMapper.update(null, new LambdaUpdateWrapper<Notification>()
                .eq(Notification::getUserId, userId)
                .eq(Notification::getIsRead, 0)
                .set(Notification::getIsRead, 1));
    }

    private NotificationResponse toResponse(Notification ntf, Map<Long, User> actors) {
        User user = actors.get(ntf.getActorId());
        BlogAuthorResponse actor = user == null ? null : new BlogAuthorResponse(
                user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(), user.getAvatar());
        return new NotificationResponse(ntf.getId(), ntf.getType(), actor,
                ntf.getIsRead() != null && ntf.getIsRead() == 1, ntf.getCreatedAt());
    }

    private Map<Long, User> loadActors(List<Notification> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = list.stream().map(Notification::getActorId).distinct().toList();
        return userMapper.selectBatchIds(ids).stream()
                .collect(java.util.stream.Collectors.toMap(User::getId, u -> u));
    }

    /** WS 实时推送（离线静默）：前端可选监听，未处理不影响轮询方案。 */
    private void pushTo(Long userId, String event) {
        try {
            UnreadCountResponse unread = unreadCount(userId);
            chatWebSocketHandler.pushToUser(userId, "{\"type\":\"notify\",\"event\":\"" + event
                    + "\",\"unreadCount\":" + unread.count() + "}");
        } catch (Exception ignored) {
            // 推送失败不影响通知落库
        }
    }
}
