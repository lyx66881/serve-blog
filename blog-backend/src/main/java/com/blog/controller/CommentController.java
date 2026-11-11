package com.blog.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.comment.CommentCreateRequest;
import com.blog.dto.comment.CommentResponse;
import com.blog.security.SecurityUtils;
import com.blog.service.CommentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "评论", description = "文章评论 / 回复（两层结构：回复统一挂在顶级评论下）")
@RestController
@RequestMapping("/api/v1")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @Operation(summary = "文章评论列表", description = "公开接口：顶级评论分页（含作者与回复数），回复通过 replies 接口按需拉取")
    @GetMapping("/blogs/{blogId}/comments")
    public ApiResponse<PageResult<CommentResponse>> listByBlog(
            @PathVariable Long blogId,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(commentService.listTopLevel(blogId, page, pageSize, SecurityUtils.currentUserIdOrNull()));
    }

    @Operation(summary = "评论的回复列表", description = "公开接口：某条评论下的回复平铺分页，按时间正序")
    @GetMapping("/comments/{id}/replies")
    public ApiResponse<PageResult<CommentResponse>> listReplies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") Long page,
            @RequestParam(defaultValue = "10") Long pageSize) {
        return ApiResponse.ok(commentService.listReplies(id, page, pageSize, SecurityUtils.currentUserIdOrNull()));
    }

    @Operation(summary = "发表评论/回复", description = "需登录；parentId 为空=顶级评论，非空=回复该顶级评论下的某人")
    @PostMapping("/blogs/{blogId}/comments")
    public ApiResponse<CommentResponse> create(@PathVariable Long blogId,
                                               @Valid @RequestBody CommentCreateRequest request) {
        return ApiResponse.ok(commentService.create(SecurityUtils.currentUserId(), blogId, request));
    }

    @Operation(summary = "删除评论", description = "需登录；仅评论作者、文章作者或管理员可删（逻辑删除，顶级评论连同回复一起删）")
    @DeleteMapping("/comments/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        commentService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok();
    }
}
