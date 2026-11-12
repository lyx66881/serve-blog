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
import com.blog.dto.admin.AdminFeedbackResponse;
import com.blog.dto.feedback.FeedbackStatusRequest;
import com.blog.service.FeedbackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "反馈管理", description = "管理员：全站意见反馈处理（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/feedbacks")
public class AdminFeedbackController {

    private final FeedbackService feedbackService;

    public AdminFeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @Operation(summary = "全站反馈列表", description = "管理员接口；status 可选 pending / processed / ignored，不传返回全部")
    @GetMapping
    public ApiResponse<PageResult<AdminFeedbackResponse>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(feedbackService.adminPage(status, page, pageSize));
    }

    @Operation(summary = "设置反馈状态", description = "管理员接口；status = pending-待处理 / processed-已处理 / ignored-已忽略")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminFeedbackResponse> setStatus(@PathVariable Long id,
                                                        @Valid @RequestBody FeedbackStatusRequest request) {
        return ApiResponse.ok(feedbackService.adminSetStatus(id, request.status()));
    }

    @Operation(summary = "删除反馈", description = "管理员接口；物理删除")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        feedbackService.adminDelete(id);
        return ApiResponse.ok(null);
    }
}
