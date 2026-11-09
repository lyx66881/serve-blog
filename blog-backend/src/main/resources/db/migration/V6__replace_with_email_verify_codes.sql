-- 统一邮箱验证码表：注册 / 重置密码 / 修改密码三个场景共用
-- （替代原 password_reset_codes，该表尚未有线上数据，直接删除）
DROP TABLE IF EXISTS password_reset_codes;

CREATE TABLE email_verify_codes (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    email      VARCHAR(100)    NOT NULL COMMENT '接收验证码的邮箱',
    scene      VARCHAR(16)     NOT NULL COMMENT '场景：register / reset / change',
    code       VARCHAR(6)      NOT NULL COMMENT '6 位数字验证码',
    expires_at DATETIME        NOT NULL COMMENT '过期时间',
    attempts   INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '验证失败次数（达到上限后作废）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email_scene (email, scene)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '邮箱验证码';
