package com.blog.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.auth.LoginRequest;
import com.blog.dto.auth.RegisterRequest;
import com.blog.dto.auth.TokenResponse;
import com.blog.dto.user.UserResponse;
import com.blog.entity.User;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtTokenProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

@Service
public class AuthService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserMapper userMapper, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.username())) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        if (req.email() != null && !req.email().isBlank()
                && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, req.email())) > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        user.setEmail(req.email() == null || req.email().isBlank() ? null : req.email());
        userMapper.insert(user);

        return buildTokenResponse(user);
    }

    public TokenResponse login(LoginRequest req) {
        // username 字段支持用户名或邮箱
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, req.username()).or().eq(User::getEmail, req.username()))
                .last("LIMIT 1"));
        if (user == null || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new BizException(ErrorCode.INVALID_CREDENTIALS);
        }
        return buildTokenResponse(user);
    }

    private TokenResponse buildTokenResponse(User user) {
        String token = tokenProvider.createToken(user.getId(), user.getUsername());
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(),
                user.getEmail(), user.getAvatar(), user.getBio(), user.getCreatedAt());
        return new TokenResponse(token, "Bearer", tokenProvider.getExpireSeconds(), userResponse);
    }
}
