-- 关注/粉丝 + 拉黑 + 私信权限设置
CREATE TABLE follows (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    follower_id  BIGINT UNSIGNED NOT NULL COMMENT '关注者',
    following_id BIGINT UNSIGNED NOT NULL COMMENT '被关注者',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '关注时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_follow (follower_id, following_id),
    KEY idx_following (following_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '关注关系表';

CREATE TABLE blocks (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    blocker_id BIGINT UNSIGNED NOT NULL COMMENT '拉黑发起者',
    blocked_id BIGINT UNSIGNED NOT NULL COMMENT '被拉黑者',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '拉黑时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_block (blocker_id, blocked_id),
    KEY idx_blocked (blocked_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '拉黑关系表';

ALTER TABLE users
    ADD COLUMN dm_policy VARCHAR(20) NOT NULL DEFAULT 'everyone'
        COMMENT '私信策略：everyone-任何人可私信；mutual_follow-仅互关好友可私信';
