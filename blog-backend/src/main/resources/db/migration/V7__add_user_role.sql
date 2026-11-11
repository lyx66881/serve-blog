-- 用户角色：user-普通用户 admin-管理员（可管理全站评论）
ALTER TABLE users
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：user-普通用户 admin-管理员' AFTER bio;
