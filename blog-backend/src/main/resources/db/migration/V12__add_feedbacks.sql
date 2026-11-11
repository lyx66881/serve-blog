-- 意见反馈表：文字与图片至少其一（提交时校验），管理端可标记处理状态
CREATE TABLE feedbacks (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    user_id       BIGINT       NOT NULL COMMENT '提交人',
    content       VARCHAR(1000) NULL COMMENT '反馈文字（可空，图片反馈可作配文）',
    image_url     VARCHAR(500)  NULL COMMENT '截图 URL（可空），存 /uploads/feedback/ 相对路径',
    image_width   INT           NULL,
    image_height  INT           NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'pending' COMMENT 'pending-待处理 / processed-已处理 / ignored-已忽略',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_fb_user (user_id, created_at),
    KEY idx_fb_status (status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci COMMENT '意见反馈';
