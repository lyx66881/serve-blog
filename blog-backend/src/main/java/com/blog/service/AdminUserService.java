package com.blog.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.common.PageResult;
import com.blog.dto.admin.AdminUserResponse;
import com.blog.entity.Blog;
import com.blog.entity.User;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.SecurityUtils;

/**
 * 管理端用户管理：列表 / 详情 / 封禁 / 角色调整 / 重置密码。
 */
@Service
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 50;

    private final UserMapper userMapper;
    private final BlogMapper blogMapper;
    private final PasswordEncoder passwordEncoder;

    public AdminUserService(UserMapper userMapper, BlogMapper blogMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 用户分页列表：keyword 匹配用户名/昵称/邮箱，可按角色、状态筛选；含博客数。
     */
    public PageResult<AdminUserResponse> list(Long page, Long pageSize, String keyword, String role, Integer status) {
        long p = page == null ? 1 : Math.max(1, page);
        long s = pageSize == null ? 10 : Math.min(Math.max(1, pageSize), MAX_PAGE_SIZE);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like(User::getUsername, kw).or().like(User::getNickname, kw).or().like(User::getEmail, kw));
        }
        if (role != null && !role.isBlank()) {
            if (!User.ROLE_USER.equals(role) && !User.ROLE_ADMIN.equals(role)) {
                throw new BizException(ErrorCode.PARAM_INVALID, "role 仅支持 user / admin");
            }
            wrapper.eq(User::getRole, role);
        }
        if (status != null) {
            if (status != User.STATUS_NORMAL && status != User.STATUS_BANNED) {
                throw new BizException(ErrorCode.PARAM_INVALID, "status 仅支持 0-正常 1-封禁");
            }
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getCreatedAt);

        Page<User> result = userMapper.selectPage(Page.of(p, s), wrapper);
        Map<Long, Long> blogCounts = loadBlogCounts(result.getRecords().stream().map(User::getId).toList());
        List<AdminUserResponse> items = result.getRecords().stream()
                .map(u -> toResponse(u, blogCounts.getOrDefault(u.getId(), 0L))).toList();
        return new PageResult<>(items, result.getTotal(), result.getCurrent(), result.getSize(), result.getPages());
    }

    /**
     * 用户详情（含博客数）。
     */
    public AdminUserResponse detail(Long id) {
        User user = requireUser(id);
        Long count = blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getAuthorId, id));
        return toResponse(user, count == null ? 0 : count);
    }

    /**
     * 封禁 / 解封用户；不能封禁自己。
     */
    @Transactional
    public AdminUserResponse updateStatus(Long operatorId, Long id, Integer status) {
        if (operatorId.equals(id)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能封禁自己");
        }
        User user = requireUser(id);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getStatus, status));
        user.setStatus(status);
        return toResponse(user, blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getAuthorId, id)));
    }

    /**
     * 调整用户角色（user / admin）；不能修改自己的角色。
     */
    @Transactional
    public AdminUserResponse updateRole(Long operatorId, Long id, String role) {
        if (operatorId.equals(id)) {
            throw new BizException(ErrorCode.FORBIDDEN, "不能修改自己的角色");
        }
        User user = requireUser(id);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getRole, role));
        user.setRole(role);
        return toResponse(user, blogMapper.selectCount(new LambdaQueryWrapper<Blog>().eq(Blog::getAuthorId, id)));
    }

    /**
     * 重置用户密码。
     */
    @Transactional
    public void resetPassword(Long id, String newPassword) {
        requireUser(id);
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, id)
                .set(User::getPassword, passwordEncoder.encode(newPassword)));
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private Map<Long, Long> loadBlogCounts(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return blogMapper.selectList(new LambdaQueryWrapper<Blog>().select(Blog::getAuthorId).in(Blog::getAuthorId, userIds))
                .stream().collect(Collectors.groupingBy(Blog::getAuthorId, Collectors.counting()));
    }

    private AdminUserResponse toResponse(User user, Long blogCount) {
        return new AdminUserResponse(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getAvatar(),
                user.getRole(),
                user.getStatus(),
                blogCount,
                user.getCreatedAt());
    }
}
