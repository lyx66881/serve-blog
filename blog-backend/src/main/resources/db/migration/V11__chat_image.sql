-- 聊天消息支持图片：新增消息类型与图片字段
ALTER TABLE messages
    ADD COLUMN msg_type     VARCHAR(10)  NOT NULL DEFAULT 'text' COMMENT '消息类型：text-文本；image-图片',
    ADD COLUMN image_url    VARCHAR(500) NULL COMMENT '图片消息的图片URL（uploads 静态路径）',
    ADD COLUMN image_width  INT          NULL COMMENT '图片宽度（像素，客户端提供，用于前端占位）',
    ADD COLUMN image_height INT          NULL COMMENT '图片高度（像素，客户端提供，用于前端占位）';
