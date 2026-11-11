package com.blog.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.blog.BlogAuthorResponse;
import com.blog.dto.social.BlockedUserResponse;
import com.blog.dto.social.BlockStateResponse;
import com.blog.dto.social.FollowStatsResponse;
import com.blog.dto.social.FollowStateResponse;
import com.blog.dto.social.FollowUserResponse;
import com.blog.entity.Block;
import com.blog.entity.Follow;
import com.blog.entity.User;
import com.blog.mapper.BlockMapper;
import com.blog.mapper.FollowMapper;
import com.blog.mapper.UserMapper;

/**
 * 关注/粉丝 + 拉黑 + 私信策略业务。
 * <p>
 * 关注与拉黑均为物理删除（无历史留痕）；重复关注/重复拉黑幂等成功。
 * 拉黑规则：A 拉黑 B 后，B 无法关注 A，且双方互发私信均被拒绝。
 * 私信策略：receiver.dmPolicy = everyone（默认）任何人可私信；mutual_follow 仅互关好友可私信。
 */
@Service
public class SocialService {

    public static final String DM_EVERYONE = "everyone";
    public static final String DM_MUTUAL_FOLLOW = "mutual_follow";

    private static final int MAX_PAGE_SIZE = 50;

    private final FollowMapper followMapper;
    private final BlockMapper blockMapper;
    private final UserMapper userMapper;

    public SocialService(FollowMapper followMapper, BlockMapper blockMapper, UserMapper userMapper) {
        this.followMapper = followMapper;
        this.blockMapper = blockMapper;
        this.userMapper = userMapper;
    }

    // ==================== 关注 ====================

    @Transactional
    public FollowStateResponse follow(Long followerId, Long targetId) {
        if (followerId.equals(targetId)) {
            throw new BizException(ErrorCode.FOLLOW_SELF);
        }
        requireUser(targetId);
        if (isBlocked(targetId, followerId)) {
            throw new BizException(ErrorCode.CANNOT_INTERACT, "无法关注该用户");
        }
        if (isFollowing(followerId, targetId)) {
            return new FollowStateResponse(true);
        }
        Follow follow = new Follow();
        follow.setFollowerId(followerId);
        follow.setFollowingId(targetId);
        follow.setCreatedAt(LocalDateTime.now());
        try {
            followMapper.insert(follow);
        } catch (DuplicateKeyException e) {
            // 并发重复关注，视为已关注
        }
        return new FollowStateResponse(true);
    }

    @Transactional
    public FollowStateResponse unfollow(Long followerId, Long targetId) {
        followMapper.delete(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, targetId));
        return new FollowStateResponse(false);
    }

    public boolean isFollowing(Long followerId, Long followingId) {
        return followMapper.selectCount(new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, followerId)
                .eq(Follow::getFollowingId, followingId)) > 0;
    }

    public boolean isMutualFollowing(Long userA, Long userB) {
        return isFollowing(userA, userB) && isFollowing(userB, userA);
    }

    /** 粉丝列表：谁关注了 targetId。 */
    public PageResult<FollowUserResponse> listFollowers(Long targetId, Long page, Long pageSize, Long viewerId) {
        requireUser(targetId);
        return listDirection(followMapper.selectPage(paged(page, pageSize), new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowingId, targetId)
                .orderByDesc(Follow::getId)), Follow::getFollowerId, viewerId);
    }

    /** 关注列表：targetId 关注了谁。 */
    public PageResult<FollowUserResponse> listFollowing(Long targetId, Long page, Long pageSize, Long viewerId) {
        requireUser(targetId);
        return listDirection(followMapper.selectPage(paged(page, pageSize), new LambdaQueryWrapper<Follow>()
                .eq(Follow::getFollowerId, targetId)
                .orderByDesc(Follow::getId)), Follow::getFollowingId, viewerId);
    }

    public FollowStatsResponse followStats(Long targetId) {
        requireUser(targetId);
        return new FollowStatsResponse(
                followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowerId, targetId)),
                followMapper.selectCount(new LambdaQueryWrapper<Follow>().eq(Follow::getFollowingId, targetId)));
    }

    // ==================== 拉黑 ====================

    @Transactional
    public BlockStateResponse block(Long blockerId, Long targetId) {
        if (blockerId.equals(targetId)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "不能拉黑自己");
        }
        requireUser(targetId);
        if (isBlocked(blockerId, targetId)) {
            return new BlockStateResponse(true);
        }
        Block block = new Block();
        block.setBlockerId(blockerId);
        block.setBlockedId(targetId);
        block.setCreatedAt(LocalDateTime.now());
        try {
            blockMapper.insert(block);
        } catch (DuplicateKeyException e) {
            // 并发重复拉黑，视为已拉黑
        }
        return new BlockStateResponse(true);
    }

    @Transactional
    public BlockStateResponse unblock(Long blockerId, Long targetId) {
        blockMapper.delete(new LambdaQueryWrapper<Block>()
                .eq(Block::getBlockerId, blockerId)
                .eq(Block::getBlockedId, targetId));
        return new BlockStateResponse(false);
    }

    /** 我是否拉黑了 target。 */
    public boolean isBlocked(Long blockerId, Long targetId) {
        return blockMapper.selectCount(new LambdaQueryWrapper<Block>()
                .eq(Block::getBlockerId, blockerId)
                .eq(Block::getBlockedId, targetId)) > 0;
    }

    /** 我的拉黑列表。 */
    public PageResult<BlockedUserResponse> listBlocked(Long blockerId, Long page, Long pageSize) {
        Page<Block> result = blockMapper.selectPage(pagedBlock(page, pageSize), new LambdaQueryWrapper<Block>()
                .eq(Block::getBlockerId, blockerId)
                .orderByDesc(Block::getId));
        Set<Long> ids = result.getRecords().stream().map(Block::getBlockedId).collect(Collectors.toSet());
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        List<BlockedUserResponse> items = result.getRecords().stream()
                .filter(b -> users.containsKey(b.getBlockedId()))
                .map(b -> new BlockedUserResponse(toAuthor(users.get(b.getBlockedId())), b.getCreatedAt()))
                .toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    // ==================== 私信策略 ====================

    @Transactional
    public com.blog.dto.user.UserResponse updateDmPolicy(Long userId, String policy) {
        String normalized = policy == null ? "" : policy.trim();
        if (!DM_EVERYONE.equals(normalized) && !DM_MUTUAL_FOLLOW.equals(normalized)) {
            throw new BizException(ErrorCode.PARAM_INVALID, "私信策略仅支持 everyone 或 mutual_follow");
        }
        requireUser(userId);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getDmPolicy, normalized));
        User updated = userMapper.selectById(userId);
        return new com.blog.dto.user.UserResponse(
                updated.getId(), updated.getUsername(),
                updated.getNickname() == null ? updated.getUsername() : updated.getNickname(),
                updated.getEmail(), updated.getAvatar(), updated.getBio(), updated.getRole(),
                updated.getDmPolicy(), updated.getCreatedAt());
    }

    /**
     * 私信校验：任一方拉黑对方则拒绝；对方设置为 mutual_follow 时要求互相关注。
     */
    public void assertCanDm(Long senderId, Long receiverId, String receiverDmPolicy) {
        if (isBlocked(receiverId, senderId) || isBlocked(senderId, receiverId)) {
            throw new BizException(ErrorCode.CANNOT_INTERACT, "无法向该用户发送私信");
        }
        if (DM_MUTUAL_FOLLOW.equals(receiverDmPolicy) && !isMutualFollowing(senderId, receiverId)) {
            throw new BizException(ErrorCode.DM_POLICY_RESTRICTED);
        }
    }

    // ==================== 私有方法 ====================

    private Page<Follow> paged(Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 20 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return Page.of(p, s);
    }

    private Page<Block> pagedBlock(Long page, Long pageSize) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 20 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);
        return Page.of(p, s);
    }

    private PageResult<FollowUserResponse> listDirection(Page<Follow> result,
                                                         java.util.function.Function<Follow, Long> otherIdGetter,
                                                         Long viewerId) {
        List<Follow> rows = result.getRecords();
        Set<Long> ids = rows.stream().map(otherIdGetter).collect(Collectors.toSet());
        Map<Long, User> users = ids.isEmpty() ? Collections.emptyMap()
                : userMapper.selectByIds(ids).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        // 注意：ids 为空时不能执行 .in(column, 空集合)，MyBatis-Plus 会生成非法 SQL（IN ()）
        Set<Long> followedByViewer = viewerId == null || ids.isEmpty() ? Collections.emptySet()
                : followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowerId, viewerId)
                        .in(Follow::getFollowingId, ids))
                        .stream().map(Follow::getFollowingId).collect(Collectors.toSet());
        Set<Long> followsViewer = viewerId == null || ids.isEmpty() ? Collections.emptySet()
                : followMapper.selectList(new LambdaQueryWrapper<Follow>()
                        .eq(Follow::getFollowingId, viewerId)
                        .in(Follow::getFollowerId, ids))
                        .stream().map(Follow::getFollowerId).collect(Collectors.toSet());
        List<FollowUserResponse> items = rows.stream()
                .filter(f -> users.containsKey(otherIdGetter.apply(f)))
                .map(f -> {
                    Long uid = otherIdGetter.apply(f);
                    return new FollowUserResponse(
                            toAuthor(users.get(uid)),
                            f.getCreatedAt(),
                            viewerId == null ? null : followedByViewer.contains(uid),
                            viewerId == null ? null : followsViewer.contains(uid));
                }).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private BlogAuthorResponse toAuthor(User user) {
        return new BlogAuthorResponse(user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(), user.getAvatar());
    }
}
