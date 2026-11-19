-- 站内私信：会话表 + 消息表
-- 会话为双人一对一：user_a_id 恒为两人中较小的用户 ID，user_b_id 为较大者，保证唯一配对
CREATE TABLE conversations (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_a_id       BIGINT UNSIGNED NOT NULL COMMENT '用户 A（两人中较小的 ID）',
    user_b_id       BIGINT UNSIGNED NOT NULL COMMENT '用户 B（两人中较大的 ID）',
    last_message    VARCHAR(200)    NULL COMMENT '最后一条消息预览（截断存储）',
    last_message_at DATETIME        NULL COMMENT '最后一条消息时间',
    last_sender_id  BIGINT UNSIGNED NULL COMMENT '最后一条消息发送者',
    unread_a        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'A 的未读数（B 发来的未读消息数）',
    unread_b        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT 'B 的未读数（A 发来的未读消息数）',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pair (user_a_id, user_b_id),
    KEY idx_a (user_a_id, last_message_at),
    KEY idx_b (user_b_id, last_message_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信会话表';

CREATE TABLE messages (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT UNSIGNED NOT NULL COMMENT '会话 ID',
    sender_id       BIGINT UNSIGNED NOT NULL COMMENT '发送者用户 ID',
    content         VARCHAR(2000)   NOT NULL COMMENT '消息内容',
    read_at         DATETIME        NULL COMMENT '对方阅读时间（NULL=未读）',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_conv (conversation_id, id),
    KEY idx_sender (sender_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '私信消息表';
