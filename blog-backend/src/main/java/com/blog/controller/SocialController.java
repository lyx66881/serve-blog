package com.blog.controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.PageResult;
import com.blog.dto.social.BlockedUserResponse;
import com.blog.dto.social.BlockStateResponse;
import com.blog.dto.social.FollowStatsResponse;
import com.blog.dto.social.FollowStateResponse;
import com.blog.dto.social.FollowUserResponse;
import com.blog.dto.social.UpdateDmPolicyRequest;
import com.blog.dto.user.UserResponse;
import com.blog.security.SecurityUtils;
import com.blog.service.SocialService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 关注/粉丝 + 拉黑 + 私信策略接口。
 * <p>
 * 关注/粉丝/统计类 GET 为公开接口；关注、取关、拉黑、取消拉黑、策略修改需登录。
 */
@Tag(name = "社交关系")
@RestController
@RequestMapping("/api/v1/users")
public class SocialController {

    private final SocialService socialService;

    public SocialController(SocialService socialService) {
        this.socialService = socialService;
    }

    /** 关注用户（幂等，重复关注返回成功；不能关注自己；被对方拉黑时 40302） */
    @PostMapping("/{id}/follow")
    public ApiResponse<FollowStateResponse> follow(@PathVariable Long id) {
        return ApiResponse.ok(socialService.follow(SecurityUtils.currentUserId(), id));
    }

    /** 取消关注（幂等） */
    @DeleteMapping("/{id}/follow")
    public ApiResponse<FollowStateResponse> unfollow(@PathVariable Long id) {
        return ApiResponse.ok(socialService.unfollow(SecurityUtils.currentUserId(), id));
    }

    /** 粉丝列表（公开）：谁关注了该用户 */
    @GetMapping("/{id}/followers")
    public ApiResponse<PageResult<FollowUserResponse>> followers(@PathVariable Long id,
                                                                 @RequestParam(required = false) Long page,
                                                                 @RequestParam(required = false) Long pageSize) {
        return ApiResponse.ok(socialService.listFollowers(id, page, pageSize, SecurityUtils.currentUserIdOrNull()));
    }

    /** 关注列表（公开）：该用户关注了谁 */
    @GetMapping("/{id}/following")
    public ApiResponse<PageResult<FollowUserResponse>> following(@PathVariable Long id,
                                                                 @RequestParam(required = false) Long page,
                                                                 @RequestParam(required = false) Long pageSize) {
        return ApiResponse.ok(socialService.listFollowing(id, page, pageSize, SecurityUtils.currentUserIdOrNull()));
    }

    /** 关注/粉丝数统计（公开） */
    @GetMapping("/{id}/follow-stats")
    public ApiResponse<FollowStatsResponse> followStats(@PathVariable Long id) {
        return ApiResponse.ok(socialService.followStats(id));
    }

    /** 拉黑用户（幂等；拉黑后对方无法关注我、双方无法互发私信） */
    @PostMapping("/{id}/block")
    public ApiResponse<BlockStateResponse> block(@PathVariable Long id) {
        return ApiResponse.ok(socialService.block(SecurityUtils.currentUserId(), id));
    }

    /** 取消拉黑（幂等） */
    @DeleteMapping("/{id}/block")
    public ApiResponse<BlockStateResponse> unblock(@PathVariable Long id) {
        return ApiResponse.ok(socialService.unblock(SecurityUtils.currentUserId(), id));
    }

    /** 我的拉黑列表（需登录） */
    @GetMapping("/me/blocks")
    public ApiResponse<PageResult<BlockedUserResponse>> blockedList(@RequestParam(required = false) Long page,
                                                                    @RequestParam(required = false) Long pageSize) {
        return ApiResponse.ok(socialService.listBlocked(SecurityUtils.currentUserId(), page, pageSize));
    }

    /** 更新我的私信策略：everyone=任何人可私信（默认）；mutual_follow=仅互关好友可私信 */
    @PutMapping("/me/dm-policy")
    public ApiResponse<UserResponse> updateDmPolicy(@Valid @RequestBody UpdateDmPolicyRequest request) {
        return ApiResponse.ok(socialService.updateDmPolicy(SecurityUtils.currentUserId(), request.policy()));
    }
}
