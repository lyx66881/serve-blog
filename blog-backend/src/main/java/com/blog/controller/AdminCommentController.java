package com.blog.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.comment.AdminCommentResponse;
import com.blog.dto.comment.CommentStatusRequest;
import com.blog.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "评论管理", description = "管理员：全站评论审核（需 admin 角色）")
@RestController
@RequestMapping("/api/v1/admin/comments")
public class AdminCommentController {

    private final CommentService commentService;

    public AdminCommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "全站评论列表", description = "管理员接口；status 可选 visible / hidden，不传返回全部，附文章标题")
    @GetMapping
    public ApiResponse<PageResult<AdminCommentResponse>> page(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(commentService.adminPage(status, page, pageSize));
    }

    @Operation(summary = "设置评论状态", description = "管理员接口；status=visible 恢复显示 / hidden 隐藏（隐藏后公开侧不可见）")
    @PutMapping("/{id}/status")
    public ApiResponse<AdminCommentResponse> setStatus(@PathVariable Long id,
                                                       @Valid @RequestBody CommentStatusRequest request) {
        return ApiResponse.ok(commentService.adminSetStatus(id, request.status()));
    }
}
