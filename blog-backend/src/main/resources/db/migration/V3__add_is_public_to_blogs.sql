-- 博客增加公开标识：公开的文章才参与最新 / 热门等公开列表排名
ALTER TABLE blogs ADD COLUMN is_public TINYINT NOT NULL DEFAULT 0
    COMMENT '是否公开 0否 1是；公开才参与公开列表排名' AFTER status;

-- 存量已发布文章视为公开，保持站点现状（新创建的博客默认不公开）
UPDATE blogs SET is_public = 1 WHERE status = 'published';
