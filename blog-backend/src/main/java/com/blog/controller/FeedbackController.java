package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.feedback.FeedbackResponse;
import com.blog.dto.feedback.SubmitFeedbackRequest;
import com.blog.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "意见反馈", description = "用户提交反馈（文字与图片至少其一）与查看自己的反馈")
@RestController
@RequestMapping("/api/v1")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "提交反馈", description = "需登录；content 与 imageId 至少其一（imageId 须为 type=feedback 上传的本人图片），皆空报 40006")
    @PostMapping("/feedback")
    public ApiResponse<FeedbackResponse> submit(@Valid @RequestBody SubmitFeedbackRequest request) {
        return ApiResponse.ok(feedbackService.submit(request));
    }

    @Operation(summary = "我的反馈列表", description = "需登录；按提交时间倒序，含处理状态")
    @GetMapping("/users/me/feedbacks")
    public ApiResponse<PageResult<FeedbackResponse>> mine(
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(feedbackService.mine(page, pageSize));
    }
}
