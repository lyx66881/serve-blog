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
import com.blog.dto.auth.EmailCodeRequest;
import com.blog.dto.auth.EmailCodeResponse;
import com.blog.dto.auth.LoginRequest;
import com.blog.dto.auth.RegisterRequest;
import com.blog.dto.auth.ResetPasswordRequest;
import com.blog.dto.auth.TokenResponse;
import com.blog.dto.user.UserResponse;
import com.blog.entity.EmailVerifyCode;
import com.blog.entity.User;
import com.blog.mapper.EmailVerifyCodeMapper;
import com.blog.mapper.UserMapper;
import com.blog.security.JwtTokenProvider;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final String SCENE_LABEL_REGISTER = "注册账号";
    private static final String SCENE_LABEL_RESET = "重置密码";
    private static final String SCENE_LABEL_CHANGE = "修改密码";

    private final UserMapper userMapper;
    private final EmailVerifyCodeMapper emailCodeMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AppProperties appProperties;
    private final MailSenderService mailSenderService;

    public AuthService(UserMapper userMapper, EmailVerifyCodeMapper emailCodeMapper,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
                       AppProperties appProperties, MailSenderService mailSenderService) {
        this.userMapper = userMapper;
        this.emailCodeMapper = emailCodeMapper;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.appProperties = appProperties;
        this.mailSenderService = mailSenderService;
    }

    /**
     * 发送邮箱验证码。
     * scene=register：邮箱必须未被注册；scene=reset：邮箱必须是已注册用户的邮箱；
     * scene=change：须登录且邮箱与当前用户绑定邮箱一致（在 Controller 层校验登录与归属）。
     */
    @Transactional
    public EmailCodeResponse sendEmailCode(EmailCodeRequest req) {
        AppProperties.EmailCode conf = appProperties.getEmailCode();
        String email = req.email().trim().toLowerCase();
        String scene = req.scene();
        String sceneLabel = switch (scene) {
            case EmailVerifyCode.SCENE_REGISTER -> SCENE_LABEL_REGISTER;
            case EmailVerifyCode.SCENE_RESET -> SCENE_LABEL_RESET;
            case EmailVerifyCode.SCENE_CHANGE -> SCENE_LABEL_CHANGE;
            default -> throw new BizException(ErrorCode.BAD_REQUEST, "未知的验证码场景");
        };

        if (EmailVerifyCode.SCENE_REGISTER.equals(scene)
                && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        if (EmailVerifyCode.SCENE_RESET.equals(scene)
                && userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) == 0) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }

        EmailVerifyCode existing = selectCodeRow(email, scene);
        if (existing != null && existing.getCreatedAt() != null
                && existing.getCreatedAt().isAfter(LocalDateTime.now().minusSeconds(conf.getResendIntervalSeconds()))) {
            throw new BizException(ErrorCode.RESET_CODE_TOO_FREQUENT);
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        LocalDateTime now = LocalDateTime.now();
        EmailVerifyCode row = new EmailVerifyCode();
        row.setEmail(email);
        row.setScene(scene);
        row.setCode(code);
        row.setExpiresAt(now.plusMinutes(conf.getExpireMinutes()));
        row.setAttempts(0);
        row.setCreatedAt(now);
        row.setUpdatedAt(now);
        emailCodeMapper.delete(new LambdaQueryWrapper<EmailVerifyCode>()
                .eq(EmailVerifyCode::getEmail, email)
                .eq(EmailVerifyCode::getScene, scene));
        emailCodeMapper.insert(row);

        boolean realSent = mailSenderService.sendVerifyCode(email, code, sceneLabel, conf.getExpireMinutes());
        log.info("Email code generated scene={} email={} realSent={} expiresAt={}", scene, email, realSent, row.getExpiresAt());
        return new EmailCodeResponse(true, maskEmail(email),
                realSent ? null : code,
                conf.getExpireMinutes() * 60L,
                conf.getResendIntervalSeconds());
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, req.username())) > 0) {
            throw new BizException(ErrorCode.USERNAME_EXISTS);
        }
        String email = req.email().trim().toLowerCase();
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getEmail, email)) > 0) {
            throw new BizException(ErrorCode.EMAIL_EXISTS);
        }
        // 校验并消费注册验证码
        verifyEmailCode(email, EmailVerifyCode.SCENE_REGISTER, req.emailCode());

        User user = new User();
        user.setUsername(req.username());
        user.setPassword(passwordEncoder.encode(req.password()));
        user.setNickname(req.nickname() == null || req.nickname().isBlank() ? req.username() : req.nickname());
        user.setEmail(email);
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

    /**
     * 重置密码：按邮箱找到用户，校验并消费 scene=reset 的验证码后更新密码。
     */
    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String email = req.email().trim().toLowerCase();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email).last("LIMIT 1"));
        if (user == null) {
            throw new BizException(ErrorCode.USER_NOT_FOUND);
        }
        verifyEmailCode(email, EmailVerifyCode.SCENE_RESET, req.code());

        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, user.getId())
                .set(User::getPassword, passwordEncoder.encode(req.newPassword())));
        log.info("Password reset completed for user {}", user.getUsername());
    }

    /**
     * 校验并消费邮箱验证码（供修改密码等场景复用）。
     * 错误累计，达到上限后验证码作废。
     */
    @Transactional
    public void verifyEmailCode(String email, String scene, String code) {
        AppProperties.EmailCode conf = appProperties.getEmailCode();
        EmailVerifyCode row = selectCodeRow(email, scene);
        if (row == null || row.getExpiresAt() == null || row.getExpiresAt().isBefore(LocalDateTime.now())
                || row.getAttempts() != null && row.getAttempts() >= conf.getMaxAttempts()) {
            deleteCodeRow(email, scene);
            throw new BizException(ErrorCode.RESET_CODE_INVALID);
        }

        if (!row.getCode().equals(code)) {
            int attempts = (row.getAttempts() == null ? 0 : row.getAttempts()) + 1;
            if (attempts >= conf.getMaxAttempts()) {
                deleteCodeRow(email, scene);
            } else {
                emailCodeMapper.update(null, new LambdaUpdateWrapper<EmailVerifyCode>()
                        .eq(EmailVerifyCode::getEmail, email)
                        .eq(EmailVerifyCode::getScene, scene)
                        .set(EmailVerifyCode::getAttempts, attempts));
            }
            log.info("Email code mismatch scene={} email={} attempts={}", scene, email, attempts);
            throw new BizException(ErrorCode.RESET_CODE_INVALID);
        }

        deleteCodeRow(email, scene);
    }

    private EmailVerifyCode selectCodeRow(String email, String scene) {
        return emailCodeMapper.selectOne(new LambdaQueryWrapper<EmailVerifyCode>()
                .eq(EmailVerifyCode::getEmail, email)
                .eq(EmailVerifyCode::getScene, scene)
                .last("LIMIT 1"));
    }

    private void deleteCodeRow(String email, String scene) {
        emailCodeMapper.delete(new LambdaQueryWrapper<EmailVerifyCode>()
                .eq(EmailVerifyCode::getEmail, email)
                .eq(EmailVerifyCode::getScene, scene));
    }

    private TokenResponse buildTokenResponse(User user) {
        String token = tokenProvider.createToken(user.getId(), user.getUsername());
        UserResponse userResponse = new UserResponse(
                user.getId(), user.getUsername(),
                user.getNickname() == null ? user.getUsername() : user.getNickname(),
                user.getEmail(), user.getAvatar(), user.getBio(), user.getCreatedAt());
        return new TokenResponse(token, "Bearer", tokenProvider.getExpireSeconds(), userResponse);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        String local = email.substring(0, at);
        String prefix = local.substring(0, Math.min(2, local.length()));
        return prefix + "***" + email.substring(at);
    }
}
