package com.blog.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.admin.AdminFeedbackStatusRequest;
import com.blog.dto.feedback.FeedbackResponse;
import com.blog.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 管理端意见反馈管理接口（SecurityConfig 层已限定 ROLE_ADMIN）。
 */
@Tag(name = "管理端-意见反馈", description = "意见反馈查看与处理（仅管理员）")
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "反馈列表", description = "可按 status 筛选（pending-待处理 processed-已处理），按提交时间倒序")
    @GetMapping
    public ApiResponse<PageResult<FeedbackResponse>> list(
            @RequestParam(required = false) Long page,
            @RequestParam(required = false) Long pageSize,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(feedbackService.adminPage(status, page, pageSize));
    }

    @Operation(summary = "更新处理状态", description = "status：pending-待处理 / processed-已处理")
    @PutMapping("/{id}/status")
    public ApiResponse<FeedbackResponse> updateStatus(@PathVariable Long id, @Valid @RequestBody AdminFeedbackStatusRequest request) {
        return ApiResponse.ok(feedbackService.updateStatus(id, request.status()));
    }

    @Operation(summary = "删除反馈", description = "逻辑删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feedbackService.delete(id);
        return ApiResponse.ok();
    }
}
