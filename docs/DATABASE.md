# 数据库设计文档

> 数据库：MySQL 8.0+，字符集 `utf8mb4`，排序规则 `utf8mb4_unicode_ci`，存储引擎 InnoDB。
> 由 Flyway 管理版本迁移，脚本位于 `blog-backend/src/main/resources/db/migration/`：
>
> - `V1__init_schema.sql` — 初始表结构
> - `V2__seed_categories.sql` — 默认分类种子数据
> - `V3__add_is_public_to_blogs.sql` — 博客公开性字段（公开才参与公开列表排名）
> - `V4__add_password_reset_codes.sql` — 忘记密码验证码表
> - `V5__add_category_learning_notes.sql` — 新增默认分类「学习笔记」
> - `V6__replace_with_email_verify_codes.sql` — 统一邮箱验证码表（替代 password_reset_codes）
>
> 应用启动时自动执行迁移（`spring.flyway.enabled=true`），禁止手工改库结构。

## 1. ER 关系

```
users 1 ──── n blogs 1 ──── n blog_tags n ──── 1 tags
                │
                └── n ──── 1 categories (可空)
users 1 ──── n uploaded_files
users 1 ──── n feedbacks (可空：匿名反馈无关联)
```

## 2. 表结构

### 2.1 users（用户表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 用户 ID |
| username | VARCHAR(50) | UNIQUE, NOT NULL | 用户名 |
| email | VARCHAR(100) | UNIQUE, NULL | 邮箱（可空，唯一索引允许多个 NULL） |
| password | VARCHAR(100) | NOT NULL | BCrypt 加密密码 |
| role | VARCHAR(20) | NOT NULL DEFAULT 'user' | 角色：user-普通用户 / admin-管理员（V7） |
| status | TINYINT | NOT NULL DEFAULT 0 | 状态：0-正常 / 1-已封禁，封禁后无法登录（V7） |
| nickname | VARCHAR(50) | NULL | 昵称 |
| avatar | VARCHAR(500) | NULL | 头像 URL |
| bio | VARCHAR(500) | NULL | 个人简介 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 逻辑删除 0/1 |
| created_at / updated_at | DATETIME | NOT NULL，默认当前时间 | updated_at 随更新自动刷新 |

### 2.2 blogs（博客表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK, AUTO_INCREMENT | 博客 ID |
| author_id | BIGINT UNSIGNED | NOT NULL, FK→users(id) | 作者 |
| title | VARCHAR(200) | NOT NULL | 标题 |
| summary | VARCHAR(500) | NULL | 摘要，缺省从正文提取前 200 字 |
| cover_image | VARCHAR(500) | NULL | 封面 URL |
| category_id | BIGINT UNSIGNED | NULL, FK→categories(id) | 分类 |
| status | VARCHAR(16) | NOT NULL DEFAULT 'draft' | draft / published / offline |
| **content** | **JSON** | **NOT NULL** | **TipTap / ProseMirror 正文 JSON，原样存取** |
| content_html | MEDIUMTEXT | NULL | 正文 HTML（可选，前端提供） |
| content_text | MEDIUMTEXT | NULL | 正文纯文本（服务端从 content 提取，用于搜索与摘要生成，不对外返回） |
| content_version | VARCHAR(20) | NOT NULL DEFAULT '1' | 内容格式版本 |
| view_count | INT UNSIGNED | NOT NULL DEFAULT 0 | 浏览量 |
| published_at | DATETIME | NULL | 首次发布时间 |
| deleted | TINYINT | NOT NULL DEFAULT 0 | 逻辑删除 |
| created_at / updated_at | DATETIME | NOT NULL | 同上 |

索引：

| 索引 | 列 | 用途 |
| --- | --- | --- |
| idx_author_status | (author_id, status) | 我的博客、用户主页博客数 |
| idx_status_published | (status, published_at) | 公开列表按发布时间排序 |
| idx_category | (category_id) | 分类筛选 |
| fk_blog_author / fk_blog_category | 外键 | 引用完整性 |

关键词搜索使用 `title LIKE '%kw%' OR content_text LIKE '%kw%'`；数据量增长后可升级为 FULLTEXT（ngram）索引，当前规模无需。

### 2.3 categories（分类表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | 分类 ID |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 分类名 |
| description | VARCHAR(200) | NULL | 描述 |
| deleted | TINYINT | DEFAULT 0 | 逻辑删除 |
| created_at / updated_at | DATETIME | NOT NULL | 同上 |

种子数据：前端开发、后端开发、工具与效率、生活随笔、学习笔记（V5）。

### 2.4 tags（标签表）

| 字段 | 类型 | 约束 | 说明 |
| --- | --- | --- | --- |
| id | BIGINT UNSIGNED | PK | 标签 ID |
| name | VARCHAR(50) | UNIQUE, NOT NULL | 标签名（应用层 trim + 去重后创建） |
| created_at | DATETIME | NOT NULL | 创建时间 |

标签由「创建/编辑博客」自动创建，不做逻辑删除。

### 2.5 blog_tags（博客-标签关联表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| blog_id | BIGINT UNSIGNED | PK(blog_id, tag_id)，FK→blogs(id) |
| tag_id | BIGINT UNSIGNED | FK→tags(id) |

索引：主键 (blog_id, tag_id) 支持按博客取标签；二级索引 idx_tag_id 支持按标签筛博客。
博客编辑标签时：先物理删除旧关联，再插入新关联（业务事务内）。

### 2.6 uploaded_files（上传文件记录表）

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK |
| uploader_id | BIGINT UNSIGNED | 上传用户，FK 语义（应用层校验） |
| biz_type | VARCHAR(20) | avatar / cover / article |
| original_name | VARCHAR(255) | 原始文件名（仅记录，不参与存储路径） |
| stored_name | VARCHAR(255) | 随机存储文件名（UUID） |
| relative_path | VARCHAR(500) | 相对上传根目录路径 |
| url | VARCHAR(500) | 访问 URL |
| content_type | VARCHAR(100) | MIME 类型 |
| file_size | BIGINT UNSIGNED | 字节数 |
| created_at | DATETIME | 上传时间 |

索引：idx_uploader (uploader_id)。删除文件 = 删磁盘文件 + 删记录（物理删除）。

### 2.7 email_verify_codes（邮箱验证码表）

注册 / 重置密码 / 修改密码三个场景统一使用（V6 起，替代 password_reset_codes）。

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| id | BIGINT UNSIGNED | PK，自增 |
| email | VARCHAR(100) | 接收验证码的邮箱 |
| scene | VARCHAR(16) | 场景：register / reset / change |
| code | VARCHAR(6) | 6 位数字验证码 |
| expires_at | DATETIME | 过期时间（默认 10 分钟） |
| attempts | INT UNSIGNED | 验证失败次数（默认上限 5 次，达到后作废） |
| created_at / updated_at | DATETIME | 同上 |

索引：PK(id)、UNIQUE(email, scene)——每个「邮箱 + 场景」仅保留最新一条。验证成功 / 过期 / 作废后物理删除该行。

## 3. 设计约定

1. **逻辑删除**：users / blogs / categories 带 `deleted` 字段，MyBatis-Plus 全局逻辑删除（查询自动追加 `deleted=0`）。blog_tags / tags / uploaded_files 无逻辑删除。
2. **唯一约束与逻辑删除**：唯一索引基于物理行，已逻辑删除的用户名/邮箱/分类名不可复用（防止历史内容归属混乱）。
3. **时间字段**：`created_at` / `updated_at` 由应用层 MyBatis-Plus 自动填充（插入填 created_at+updated_at，更新填 updated_at），数据库层亦有默认值兜底。
4. **主键**：全部 AUTO_INCREMENT BIGINT，JS Number 安全范围内。
5. **外键**：blogs→users/categories、blog_tags→blogs/tags 建有外键约束；因使用逻辑删除，物理删除不会破坏引用。
6. **JSON 列**：`blogs.content` 为 MySQL 原生 JSON 类型，MySQL 校验合法性；MyBatis-Plus 以 String 映射，插入时校验必须是 JSON 对象。
