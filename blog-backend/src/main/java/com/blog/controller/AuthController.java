package com.blog.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.blog.common.ApiResponse;
import com.blog.dto.auth.LoginRequest;
import com.blog.dto.auth.RegisterRequest;
import com.blog.dto.auth.TokenResponse;
import com.blog.security.InMemoryTokenBlacklist;
import com.blog.security.JwtTokenProvider;
import com.blog.service.AuthService;

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
    private final JwtTokenProvider tokenProvider;
    private final InMemoryTokenBlacklist tokenBlacklist;

    public AuthController(AuthService authService, JwtTokenProvider tokenProvider,
                          InMemoryTokenBlacklist tokenBlacklist) {
        this.authService = authService;
        this.tokenProvider = tokenProvider;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Operation(summary = "注册", description = "注册成功后直接返回 Token（自动登录）")
    @PostMapping("/register")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @Operation(summary = "登录", description = "username 字段支持用户名或邮箱")
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
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
