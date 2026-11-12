-- 站内通知：type 预留扩展（follow-新关注；后续可加 like/comment/system 等）
CREATE TABLE notifications (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    user_id    BIGINT UNSIGNED NOT NULL COMMENT '接收人',
    type       VARCHAR(20)  NOT NULL DEFAULT 'follow' COMMENT '类型：follow-新关注',
    actor_id   BIGINT UNSIGNED NOT NULL COMMENT '触发者（如关注你的人）',
    is_read    TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未读 1-已读',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_ntf_user_read (user_id, is_read, id),
    KEY idx_ntf_actor (actor_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT '站内通知';
