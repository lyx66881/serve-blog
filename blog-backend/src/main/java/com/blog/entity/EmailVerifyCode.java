package com.blog.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/**
 * 邮箱验证码（scene：register / reset / change）。
 * 每个「邮箱 + 场景」仅保留最新一条验证码，验证成功或作废后删除。
 */
@TableName("email_verify_codes")
public class EmailVerifyCode {

    public static final String SCENE_REGISTER = "register";
    public static final String SCENE_RESET = "reset";
    public static final String SCENE_CHANGE = "change";

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String email;

    private String scene;

    private String code;

    private LocalDateTime expiresAt;

    private Integer attempts;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getScene() { return scene; }
    public void setScene(String scene) { this.scene = scene; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public Integer getAttempts() { return attempts; }
    public void setAttempts(Integer attempts) { this.attempts = attempts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
