-- =====================================================================
-- 博客平台初始表结构
-- MySQL 8.0+ / InnoDB / utf8mb4
-- 说明：
--   1. users / blogs / categories 使用逻辑删除（deleted 字段）
--   2. blogs.content 使用 MySQL JSON 类型存储 TipTap / ProseMirror JSON
--   3. 所有表统一 created_at / updated_at（DATETIME）
-- =====================================================================

CREATE TABLE users (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    username    VARCHAR(50)     NOT NULL COMMENT '用户名，唯一',
    email       VARCHAR(100)    NULL COMMENT '邮箱，唯一可空',
    password    VARCHAR(100)    NOT NULL COMMENT 'BCrypt 加密密码',
    nickname    VARCHAR(50)     NULL COMMENT '昵称',
    avatar      VARCHAR(500)    NULL COMMENT '头像 URL',
    bio         VARCHAR(500)    NULL COMMENT '个人简介',
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_email (email)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表';

CREATE TABLE categories (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name        VARCHAR(50)     NOT NULL COMMENT '分类名，唯一',
    description VARCHAR(200)    NULL COMMENT '分类描述',
    deleted     TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '博客分类表';

CREATE TABLE tags (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name       VARCHAR(50)     NOT NULL COMMENT '标签名，唯一',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '标签表';

CREATE TABLE blogs (
    id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    author_id       BIGINT UNSIGNED NOT NULL COMMENT '作者用户 ID',
    title           VARCHAR(200)    NOT NULL COMMENT '标题',
    summary         VARCHAR(500)    NULL COMMENT '摘要，为空时服务端从正文提取',
    cover_image     VARCHAR(500)    NULL COMMENT '封面图 URL',
    category_id     BIGINT UNSIGNED NULL COMMENT '分类 ID',
    status          VARCHAR(16)     NOT NULL DEFAULT 'draft' COMMENT '状态：draft-草稿 published-已发布 offline-已下架',
    content         JSON            NOT NULL COMMENT '博客正文，TipTap / ProseMirror JSON',
    content_html    MEDIUMTEXT      NULL COMMENT '正文 HTML（可选，由前端提供）',
    content_text    MEDIUMTEXT      NULL COMMENT '正文纯文本（服务端从 content 提取，用于搜索与摘要）',
    content_version VARCHAR(20)     NOT NULL DEFAULT '1' COMMENT '内容格式版本',
    view_count      INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '浏览量',
    published_at    DATETIME        NULL COMMENT '首次发布时间',
    deleted         TINYINT         NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常 1-已删除',
    created_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_author_status (author_id, status),
    KEY idx_status_published (status, published_at),
    KEY idx_category (category_id),
    CONSTRAINT fk_blog_author FOREIGN KEY (author_id) REFERENCES users (id),
    CONSTRAINT fk_blog_category FOREIGN KEY (category_id) REFERENCES categories (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '博客表';

CREATE TABLE blog_tags (
    blog_id BIGINT UNSIGNED NOT NULL COMMENT '博客 ID',
    tag_id  BIGINT UNSIGNED NOT NULL COMMENT '标签 ID',
    PRIMARY KEY (blog_id, tag_id),
    KEY idx_tag_id (tag_id),
    CONSTRAINT fk_bt_blog FOREIGN KEY (blog_id) REFERENCES blogs (id),
    CONSTRAINT fk_bt_tag FOREIGN KEY (tag_id) REFERENCES tags (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '博客与标签关联表';

CREATE TABLE uploaded_files (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    uploader_id   BIGINT UNSIGNED NOT NULL COMMENT '上传用户 ID',
    biz_type      VARCHAR(20)     NOT NULL COMMENT '业务类型：avatar-头像 cover-封面 article-正文图片',
    original_name VARCHAR(255)    NULL COMMENT '原始文件名',
    stored_name   VARCHAR(255)    NOT NULL COMMENT '存储文件名（随机生成）',
    relative_path VARCHAR(500)    NOT NULL COMMENT '相对存储路径',
    url           VARCHAR(500)    NOT NULL COMMENT '访问 URL',
    content_type  VARCHAR(100)    NULL COMMENT '文件 MIME 类型',
    file_size     BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '文件大小（字节）',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_uploader (uploader_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '上传文件记录表';
