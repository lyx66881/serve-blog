-- 用户封禁状态：0-正常 1-已封禁（封禁后无法登录，已有 token 立即失效）
ALTER TABLE users
    ADD COLUMN status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-正常 1-已封禁' AFTER role;
