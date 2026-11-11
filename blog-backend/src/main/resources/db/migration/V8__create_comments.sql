-- 文章评论表：支持顶级评论与楼中楼回复（parent_id 指向顶级评论）
CREATE TABLE comments (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    blog_id          BIGINT UNSIGNED NOT NULL COMMENT '博客 ID',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '评论者用户 ID',
    parent_id        BIGINT UNSIGNED NULL COMMENT '父评论 ID（顶级评论为 NULL，回复统一挂顶级评论下）',
    reply_to_user_id BIGINT UNSIGNED NULL COMMENT '被回复用户 ID（回复某条回复时=该回复作者）',
    content          VARCHAR(1000)   NOT NULL COMMENT '评论内容',
    status           VARCHAR(16)     NOT NULL DEFAULT 'visible' COMMENT '状态：visible-正常 hidden-已隐藏（管理端屏蔽）',
    deleted          TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_blog_parent (blog_id, parent_id, status),
    KEY idx_user (user_id),
    KEY idx_parent (parent_id),
    CONSTRAINT fk_comment_blog FOREIGN KEY (blog_id) REFERENCES blogs (id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_comment_parent FOREIGN KEY (parent_id) REFERENCES comments (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文章评论表';
