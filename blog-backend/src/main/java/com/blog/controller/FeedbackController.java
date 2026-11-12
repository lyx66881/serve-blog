package com.blog.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.feedback.FeedbackCreateRequest;
import com.blog.dto.feedback.FeedbackResponse;
import com.blog.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "意见反馈", description = "用户提交意见反馈")
@RestController
@RequestMapping("/api/v1/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "提交意见反馈", description = "公开接口；登录用户自动关联账号，匿名用户可留联系方式")
    @PostMapping
    public ApiResponse<FeedbackResponse> create(@Valid @RequestBody FeedbackCreateRequest request) {
        return ApiResponse.ok(feedbackService.create(request));
    }
}
