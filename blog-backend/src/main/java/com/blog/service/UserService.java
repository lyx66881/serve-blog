package com.blog.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.user.ChangePasswordRequest;
import com.blog.dto.user.UpdateProfileRequest;
import com.blog.dto.user.UserProfileResponse;
import com.blog.dto.user.UserResponse;
import com.blog.entity.Blog;
import com.blog.entity.User;
import com.blog.mapper.BlogMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.UserPrincipal;
import com.blog.security.SecurityUtils;
import com.blog.storage.FileStorageService;
import com.blog.storage.StoredFile;

@Service
public class UserService {

    private final UserMapper userMapper;
    private final BlogMapper blogMapper;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, BlogMapper blogMapper, FileStorageService fileStorageService,
                       PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.blogMapper = blogMapper;
        this.fileStorageService = fileStorageService;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse getCurrentUser() {
        Long userId = SecurityUtils.currentUserId();
        User user = requireUser(userId);
        return toSelfResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest req) {
        Long userId = SecurityUtils.currentUserId();
        requireUser(userId);

        LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>().eq(User::getId, userId);
        if (req.nickname() != null) {
            wrapper.set(User::getNickname, req.nickname());
        }
        if (req.bio() != null) {
            wrapper.set(User::getBio, req.bio());
        }
        if (req.avatar() != null) {
            wrapper.set(User::getAvatar, req.avatar());
        }
        userMapper.update(null, wrapper);

        return toSelfResponse(requireUser(userId));
    }

    /**
     * 上传头像并更新当前用户头像。
     */
    @Transactional
    public UserResponse updateAvatar(MultipartFile file) {
        Long userId = SecurityUtils.currentUserId();
        requireUser(userId);

        StoredFile stored = fileStorageService.store(file, "avatar");
        userMapper.update(null, new LambdaUpdateWrapper<User>().eq(User::getId, userId)
                .set(User::getAvatar, stored.url()));

        return toSelfResponse(requireUser(userId));
    }

    /**
     * 修改密码：校验原密码后更新为新密码。
     */
    @Transactional
    public void changePassword(ChangePasswordRequest req) {
        User user = requireUser(SecurityUtils.currentUserId());
        if (!passwordEncoder.matches(req.oldPassword(), user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_OLD_PASSWORD);
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(req.newPassword())));
    }

    /**
     * 用户公开主页：基本信息 + 已发布博客数。
     */
    public UserProfileResponse getProfile(Long userId) {
        User user = requireUser(userId);
        long blogCount = blogMapper.selectCount(new LambdaQueryWrapper<Blog>()
                .eq(Blog::getAuthorId, userId)
                .eq(Blog::getStatus, Blog.STATUS_PUBLISHED));
        return new UserProfileResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getAvatar(), user.getBio(), user.getCreatedAt(), blogCount);
    }

    private User requireUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    private UserResponse toSelfResponse(User user) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getNickname(),
                user.getEmail(), user.getAvatar(), user.getBio(), user.getCreatedAt());
    }

    /**
     * 供 BlogService 使用的公开作者信息映射。
     */
    public UserPrincipal authorOf(User user) {
        return new UserPrincipal(user.getId(), user.getUsername());
    }

    public User getUserEntity(Long userId) {
        return userMapper.selectById(userId);
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }
}
