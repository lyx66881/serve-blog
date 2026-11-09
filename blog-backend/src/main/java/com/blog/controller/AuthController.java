package com.blog.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.common.BizException;
import com.blog.common.ErrorCode;
import com.blog.dto.auth.EmailCodeRequest;
import com.blog.dto.auth.EmailCodeResponse;
import com.blog.dto.auth.LoginRequest;
import com.blog.dto.auth.RegisterRequest;
import com.blog.dto.auth.ResetPasswordRequest;
import com.blog.dto.auth.TokenResponse;
import com.blog.entity.EmailVerifyCode;
import com.blog.security.InMemoryTokenBlacklist;
import com.blog.security.JwtTokenProvider;
import com.blog.security.SecurityUtils;
import com.blog.service.AuthService;
import com.blog.service.UserService;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "认证", description = "注册 / 登录 / 退出")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final InMemoryTokenBlacklist tokenBlacklist;

    public AuthController(AuthService authService, UserService userService, JwtTokenProvider tokenProvider,
                          InMemoryTokenBlacklist tokenBlacklist) {
        this.authService = authService;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Operation(summary = "发送邮箱验证码",
            description = "scene=register（公开）：邮箱未被注册才发送；scene=reset（公开）：邮箱须为已注册用户的邮箱；"
                    + "scene=change（需登录）：邮箱须与当前用户绑定邮箱一致。"
                    + "60 秒内不可重复发送，验证码 10 分钟有效，连续错 5 次作废。"
                    + "未配置 SMTP 时为模拟模式，验证码通过 devCode 字段返回。")
    @PostMapping("/email-code")
    public ApiResponse<EmailCodeResponse> emailCode(@Valid @RequestBody EmailCodeRequest request) {
        if (EmailVerifyCode.SCENE_CHANGE.equals(request.scene())) {
            Long userId = SecurityUtils.currentUserIdOrNull();
            if (userId == null) {
                throw new BizException(ErrorCode.UNAUTHORIZED);
            }
            String boundEmail = userService.getUserEntity(userId).getEmail();
            if (boundEmail == null || boundEmail.isBlank()) {
                throw new BizException(ErrorCode.EMAIL_NOT_BOUND);
            }
            if (!boundEmail.equalsIgnoreCase(request.email().trim())) {
                throw new BizException(ErrorCode.FORBIDDEN, "邮箱与当前账号绑定邮箱不一致");
            }
        }
        return ApiResponse.ok(authService.sendEmailCode(request));
    }

    @Operation(summary = "注册", description = "邮箱必填，须先通过 scene=register 的邮箱验证码；成功后直接返回 Token（自动登录）")
    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "登录", description = "username 字段支持用户名或邮箱")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @Operation(summary = "重置密码", description = "邮箱 + scene=reset 的验证码 + 新密码；验证码连续错 5 次作废")
    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ApiResponse.ok();
    }

    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单（内存实现，重启后失效）")
    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String authorization) {
        String token = authorization.startsWith(BEARER_PREFIX)
                ? authorization.substring(BEARER_PREFIX.length()) : authorization;
        try {
            tokenBlacklist.add(token, tokenProvider.getExpiration(token).getTime());
        } catch (JwtException | IllegalArgumentException ignored) {
            // token 已无效，视为已退出
        }
        return ApiResponse.ok();
    }
}
