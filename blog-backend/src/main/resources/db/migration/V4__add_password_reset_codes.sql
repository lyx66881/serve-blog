-- 忘记密码验证码表：每个用户仅保留最新一条验证码，验证成功或过期后删除
CREATE TABLE password_reset_codes (
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '用户 ID（对应 users.id）',
    code       VARCHAR(6)      NOT NULL COMMENT '6 位数字验证码',
    expires_at DATETIME        NOT NULL COMMENT '过期时间',
    attempts   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '验证失败次数（达到上限后作废）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (user_id),
    KEY idx_code (code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '忘记密码验证码';
