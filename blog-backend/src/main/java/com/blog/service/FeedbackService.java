package com.blog.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.feedback.FeedbackCreateRequest;
import com.blog.dto.feedback.FeedbackResponse;
import com.blog.entity.Feedback;
import com.blog.entity.User;
import com.blog.mapper.FeedbackMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtils;

@Service
public class FeedbackService {

    private static final int MAX_PAGE_SIZE = 50;

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;

    public FeedbackService(FeedbackMapper feedbackMapper, UserMapper userMapper) {
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
    }

    /**
     * 提交反馈：公开接口。登录用户自动关联账号，匿名用户填写联系方式。
     */
    @Transactional
    public FeedbackResponse create(FeedbackCreateRequest req) {
        Long userId = SecurityUtils.currentUserIdOrNull();
        Feedback feedback = new Feedback();
        feedback.setUserId(userId);
        feedback.setContact(req.contact() == null || req.contact().isBlank() ? null : req.contact().trim());
        feedback.setContent(req.content().trim());
        feedback.setStatus(Feedback.STATUS_PENDING);
        feedbackMapper.insert(feedback);
        return toResponse(feedback, loadUserMap(Collections.singletonList(feedback.getUserId())));
    }

    /**
     * 管理端分页列表，可按处理状态筛选。
     */
    public PageResult<FeedbackResponse> adminPage(String status, Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 10 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        LambdaQueryWrapper<Feedback> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) {
            if (!Feedback.STATUS_PENDING.equals(status) && !Feedback.STATUS_PROCESSED.equals(status)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 pending / processed");
            }
            wrapper.eq(Feedback::getStatus, status);
        }
        wrapper.orderByDesc(Feedback::getCreatedAt);

        Page<Feedback> result = feedbackMapper.selectPage(Page.of(p, s), wrapper);
        Set<Long> userIds = result.getRecords().stream()
                .map(Feedback::getUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
        List<FeedbackResponse> items = result.getRecords().stream()
                .map(f -> toResponse(f, users)).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    /**
     * 更新处理状态（pending / processed）。
     */
    @Transactional
    public FeedbackResponse updateStatus(Long id, String status) {
        if (!Feedback.STATUS_PENDING.equals(status) && !Feedback.STATUS_PROCESSED.equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 pending / processed");
        }
        Feedback feedback = requireFeedback(id);
        feedback.setStatus(status);
        feedbackMapper.updateById(feedback);
        return toResponse(feedback, loadUserMap(Collections.singletonList(feedback.getUserId())));
    }

    @Transactional
    public void delete(Long id) {
        requireFeedback(id);
        feedbackMapper.deleteById(id);
    }

    private Feedback requireFeedback(Long id) {
        Feedback feedback = feedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        return feedback;
    }

    private Map<Long, User> loadUserMap(List<Long> userIds) {
        Set<Long> ids = userIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectByIds(ids).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));
    }

    private FeedbackResponse toResponse(Feedback feedback, Map<Long, User> users) {
        User user = feedback.getUserId() == null ? null : users.get(feedback.getUserId());
        return new FeedbackResponse(
                feedback.getId(),
                feedback.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getAvatar(),
                feedback.getContact(),
                feedback.getContent(),
                feedback.getStatus(),
                feedback.getCreatedAt());
    }
}
