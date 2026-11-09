package com.blog.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.config.AppProperties;
import com.blog.dto.auth.ForgotPasswordRequest;
import com.blog.dto.auth.ForgotPasswordResponse;
import com.blog.dto.auth.LoginRequest;
import com.blog.dto.auth.RegisterRequest;
import com.blog.dto.auth.ResetPasswordRequest;
import com.blog.dto.auth.TokenResponse;
import com.blog.dto.user.UserResponse;
import com.blog.entity.PasswordResetCode;
import com.blog.entity.User;
import com.blog.mapper.PasswordResetCodeMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtTokenProvider;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final PasswordResetCodeMapper resetCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;

    public AuthService(UserMapper userMapper, PasswordResetCodeMapper resetCodeMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
                       AppProperties appProperties) {
        this.userMapper = userMapper;
        this.resetCodeMapper = resetCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.appProperties = appProperties;
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

    /**
     * 忘记密码：按用户名或邮箱查找用户，生成 6 位验证码（每用户仅保留最新一条）。
     * 当前未接入邮件服务，验证码以「模拟发送」方式随响应返回（expose-code 可关闭）。
     */
    @Transactional
    public ForgotPasswordResponse forgotPassword(ForgotPasswordRequest req) {
        AppProperties.PasswordReset conf = appProperties.getPasswordReset();
        User user = findUserByAccount(req.account());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        PasswordResetCode existing = resetCodeMapper.selectById(user.getId());
        if (existing != null && existing.getCreatedAt() != null
                && existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(conf.getResendIntervalSeconds()))) {
            throw new BizException(ErrorCode.RESET_CODE_TOO_FREQUENT);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        PasswordResetCode row = new PasswordResetCode();
        row.setUserId(user.getId());
        row.setCode(code);
        row.setExpiresAt(now.plusMinutes(conf.getExpireMinutes()));
        row.setAttempts(0);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        resetCodeMapper.deleteById(user.getId());
        resetCodeMapper.insert(row);

        log.info("Password reset code generated for user {} (expires at {})", user.getUsername(), row.getExpiresAt());
        return new ForgotPasswordResponse(true,
                conf.isExposeCode() ? code : null,
                conf.getExpireMinutes() * 60L,
                conf.getResendIntervalSeconds());
    }

    /**
     * 重置密码：校验验证码（错误累计，达到上限作废），通过后更新密码并删除验证码。
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        AppProperties.PasswordReset conf = appProperties.getPasswordReset();
        User user = findUserByAccount(req.account());
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        PasswordResetCode row = resetCodeMapper.selectById(user.getId());
        if (row == null || row.getExpiresAt() == null || row.getExpiresAt().isBefore(LocalDateTime.now())
                || row.getAttempts() != null && row.getAttempts() >= conf.getMaxAttempts()) {
            resetCodeMapper.deleteById(user.getId());
            throw new BizException(ErrorCode.RESET_CODE_INVALID);
        }

        if (!row.getCode().equals(req.code())) {
            int attempts = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
            if (attempts >= conf.getMaxAttempts()) {
                resetCodeMapper.deleteById(user.getId());
            } else {
                resetCodeMapper.update(null, new LambdaUpdateWrapper<PasswordResetCode>()
                        .eq(PasswordResetCode::getUserId, user.getId())
                        .set(PasswordResetCode::getAttempts, attempts));
            }
            log.info("Password reset code mismatch for user {}, attempts={}", user.getUsername(), attempts);
            throw new BizException(ErrorCode.RESET_CODE_INVALID);
        }

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(req.newPassword())));
        resetCodeMapper.deleteById(user.getId());
        log.info("Password reset completed for user {}", user.getUsername());
    }

    private User findUserByAccount(String account) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>()
                .and(w -> w.eq(User::getUsername, account).or().eq(User::getEmail, account))
                .last("LIMIT 1"));
    }
}
