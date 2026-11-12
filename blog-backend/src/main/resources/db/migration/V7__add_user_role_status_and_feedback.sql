-- =====================================================================
-- V7: 管理端支持
--   1. users 增加 role（user/admin）与 status（0-正常 1-封禁）字段
--   2. 新增 feedbacks 意见反馈表
-- 管理员账号由应用启动时的 AdminInitializer 按 app.admin 配置创建/提升，
-- 不在迁移中写死 BCrypt 哈希。
-- =====================================================================

ALTER TABLE users
    ADD COLUMN role   VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户 admin-管理员' AFTER password,
    ADD COLUMN status TINYINT     NOT NULL DEFAULT 0      COMMENT '状态：0-正常 1-已封禁' AFTER role;

CREATE TABLE feedbacks (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NULL COMMENT '提交用户 ID，匿名反馈为空',
    contact    VARCHAR(100)    NULL COMMENT '联系方式（邮箱/QQ 等），匿名时填写',
    content    VARCHAR(1000)   NOT NULL COMMENT '反馈内容',
    status     VARCHAR(20)     NOT NULL DEFAULT 'pending' COMMENT '处理状态：pending-待处理 processed-已处理',
    deleted    TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_status_created (status, created_at),
    CONSTRAINT fk_feedback_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '意见反馈表';
