package com.blog.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.admin.AdminFeedbackResponse;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.feedback.FeedbackResponse;
import com.blog.dto.feedback.SubmitFeedbackRequest;
import com.blog.entity.Feedback;
import com.blog.entity.UploadedFile;
import com.blog.entity.User;
import com.blog.mapper.FeedbackMapper;
import com.blog.mapper.UploadedFileMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 意见反馈业务：文字与图片至少其一即可提交；本人可见自己的反馈，管理端可查看全部并维护状态。
 */
@Service
public class FeedbackService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_CONTENT_LENGTH = 1000;
    private static final String FB_BIZ_TYPE = "feedback";

    private final FeedbackMapper feedbackMapper;
    private final UserMapper userMapper;
    private final UploadedFileMapper uploadedFileMapper;

    public FeedbackService(FeedbackMapper feedbackMapper, UserMapper userMapper,
                           UploadedFileMapper uploadedFileMapper) {
        this.feedbackMapper = feedbackMapper;
        this.userMapper = userMapper;
        this.uploadedFileMapper = uploadedFileMapper;
    }

    /**
     * 提交反馈：content 与 imageId 至少其一；图片须为本人上传且 bizType=feedback。
     */
    public FeedbackResponse submit(SubmitFeedbackRequest req) {
        String content = req.content() == null ? "" : req.content().trim();
        if (content.isEmpty() && req.imageId() == null) {
            throw new BizException(ErrorCode.PARAM_INVALID, "反馈需要至少包含文字或图片之一");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BizException(ErrorCode.PARAM_INVALID, "反馈内容最长 1000 字符");
        }

        String imageUrl = null;
        if (req.imageId() != null) {
            UploadedFile file = uploadedFileMapper.selectById(req.imageId());
            if (file == null) {
                throw new BizException(ErrorCode.FILE_NOT_FOUND);
            }
            if (!file.getUploaderId().equals(SecurityUtils.currentUserId())) {
                throw new BizException(ErrorCode.FORBIDDEN, "只能使用自己上传的图片");
            }
            if (!FB_BIZ_TYPE.equals(file.getBizType())) {
                throw new BizException(ErrorCode.PARAM_INVALID, "仅支持 feedback 类型上传的图片");
            }
            imageUrl = file.getUrl();
        }

        Feedback fb = new Feedback();
        fb.setUserId(SecurityUtils.currentUserId());
        fb.setContent(content.isEmpty() ? null : content);
        fb.setImageUrl(imageUrl);
        fb.setImageWidth(req.imageWidth());
        fb.setImageHeight(req.imageHeight());
        fb.setStatus("pending");
        feedbackMapper.insert(fb);
        return toResponse(fb, Map.of());
    }

    /** 我的反馈列表：按提交时间倒序。 */
    public PageResult<FeedbackResponse> mine(Long page, Long pageSize) {
        Page<Feedback> result = feedbackMapper.selectPage(page(page, pageSize),
                new LambdaQueryWrapper<Feedback>()
                        .eq(Feedback::getUserId, SecurityUtils.currentUserId())
                        .orderByDesc(Feedback::getCreatedAt));
        Map<Long, User> users = loadUsers(result.getRecords());
        return PageResult.of(result, fb -> toResponse(fb, users));
    }

    /** 管理端：全站反馈分页，可按 status 筛选，附提交人信息（扁平结构）。 */
    public PageResult<AdminFeedbackResponse> adminPage(String status, Long page, Long pageSize) {
        Page<Feedback> result = feedbackMapper.selectPage(page(page, pageSize),
                new LambdaQueryWrapper<Feedback>()
                        .eq(status != null && !status.isBlank(), Feedback::getStatus, status)
                        .orderByDesc(Feedback::getCreatedAt));
        Map<Long, User> users = loadUsers(result.getRecords());
        return PageResult.of(result, fb -> toAdminResponse(fb, users));
    }

    /** 管理端：更新反馈处理状态（pending-待处理 / processed-已处理 / ignored-已忽略）。 */
    public AdminFeedbackResponse adminSetStatus(Long id, String status) {
        if (!"pending".equals(status) && !"processed".equals(status) && !"ignored".equals(status)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 pending / processed / ignored");
        }
        Feedback fb = feedbackMapper.selectById(id);
        if (fb == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        fb.setStatus(status);
        feedbackMapper.updateById(fb);
        return toAdminResponse(fb, loadUsers(List.of(fb)));
    }

    /** 管理端：删除反馈（物理删除）。 */
    public void adminDelete(Long id) {
        if (feedbackMapper.selectById(id) == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "反馈不存在");
        }
        feedbackMapper.deleteById(id);
    }

    private Page<Feedback> page(Long page, Long pageSize) {
        return new Page<>(Math.max(page, 1), Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE));
    }

    private Map<Long, User> loadUsers(List<Feedback> list) {
        if (list.isEmpty()) {
            return Map.of();
        }
        List<Long> userIds = list.stream().map(Feedback::getUserId).distinct().toList();
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    private FeedbackResponse toResponse(Feedback fb, Map<Long, User> users) {
        User user = users.get(fb.getUserId());
        BlogAuthorResponse author = user == null ? null : new BlogAuthorResponse(
                user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(), user.getAvatar());
        return new FeedbackResponse(fb.getId(), author, fb.getContent(),
                fb.getImageUrl(), fb.getImageWidth(), fb.getImageHeight(), fb.getStatus(), fb.getCreatedAt());
    }

    private AdminFeedbackResponse toAdminResponse(Feedback fb, Map<Long, User> users) {
        User user = users.get(fb.getUserId());
        return new AdminFeedbackResponse(
                fb.getId(), fb.getUserId(),
                user == null ? null : user.getUsername(),
                user == null ? null : user.getNickname(),
                user == null ? null : user.getAvatar(),
                null,
                fb.getContent(), fb.getImageUrl(), fb.getImageWidth(), fb.getImageHeight(),
                fb.getStatus(), fb.getCreatedAt());
    }
}
