# 博客服务端功能记录（FUNCTION_LOG）

> **本文件是 blog-backend 服务端项目的功能台账。**
> **规则：此后任何对本项目的功能变更、接口调整、数据修改、部署变更，都必须在本文件末尾「变更记录」中追加条目。**
> 首次创建：2026-11-11，基于对源码与线上部署的完整扫描。

---

## 一、项目概览

| 项 | 内容 |
|---|---|
| 项目名 | blog-backend（博客平台服务端） |
| 语言/运行时 | Java 21（线上 JVM 为 /usr/local/java） |
| 框架 | Spring Boot 3.5.5（Web/MVC、Validation、Security） |
| ORM | MyBatis-Plus（驼峰映射、逻辑删除、分页插件） |
| 数据库 | MySQL 8（线上为服务器本机 MySQL 8，端口 3307） |
| 数据库迁移 | Flyway（classpath:db/migration） |
| 认证 | JWT（HS256，无状态，内存黑名单登出） |
| API 文档 | SpringDoc OpenAPI（/swagger-ui.html、/v3/api-docs） |
| 邮件 | Spring Mail（验证码邮件，可模拟模式） |
| 构建 | Maven（pom.xml） |

## 二、线上部署信息（2026-11-11 核查）

| 项 | 内容 |
|---|---|
| 服务器 | 106.15.170.114（阿里云，OneInStack LAMP 镜像，SSH 用户 root） |
| 后端进程 | `/usr/local/java/bin/java -Xms256m -Xmx512m -jar /opt/blog-backend/blog-backend-1.0.0.jar` |
| 后端端口 | 8080（公网可访问，http://106.15.170.114:8080） |
| 数据库 | MySQL 8，`/usr/local/mysql8`，端口 3307（仅本机可连），库名 `blog_platform` |
| 数据库账号 | blog / blog123456（应用账号）；root 密码见服务器 /root/ReadMe |
| 上传目录 | /data/blog/uploads（UPLOAD_BASE_DIR），经 WebConfig 映射为静态资源 `/uploads/**` |
| 环境变量 | DB_URL、DB_USERNAME、DB_PASSWORD、JWT_SECRET、JWT_EXPIRE_HOURS=72、UPLOAD_BASE_DIR、MAIL_ENABLED=true |
| 前端 | Apache 2.4（/usr/local/apache）承载 web-blog 前端，80 端口 |
| 旧 MySQL 5.6 | /usr/local/mysql，端口 3306，与本博客系统无关 |
| 数据备份 | /root/db-backups/（mysqldump 定期手动备份，改动前必备份） |

## 三、目录结构

```
blog-backend/
├── pom.xml                          # Maven 依赖
└── src/main/
    ├── java/com/blog/
    │   ├── BlogApplication.java     # 启动类
    │   ├── common/                  # 通用层
    │   │   ├── ApiResponse.java     # 统一响应包裹 {code, message, data}
    │   │   ├── BizException.java    # 业务异常
    │   │   ├── ErrorCode.java       # 错误码枚举
    │   │   ├── GlobalExceptionHandler.java  # 全局异常处理
    │   │   ├── PageResult.java      # 分页结果 {list, total, page, pageSize, pages}
    │   │   └── util/TipTapTextExtractor.java # TipTap JSON 提取纯文本/摘要
    │   ├── config/                  # 配置层
    │   │   ├── AppProperties.java   # app.* 配置绑定（jwt/upload/cors/mail/email-code）
    │   │   ├── SecurityConfig.java  # Spring Security + JWT 无状态配置
    │   │   ├── WebConfig.java       # 静态资源映射 /uploads/** → 本地上传目录；CORS
    │   │   ├── MybatisPlusConfig.java # 分页插件等
    │   │   └── OpenApiConfig.java   # Swagger 分组
    │   ├── controller/              # 控制器（6 个）
    │   ├── dto/                     # 请求/响应 DTO（auth/blog/category/tag/upload/user）
    │   ├── entity/                  # 实体（User/Blog/Category/Tag/BlogTag/UploadedFile/EmailVerifyCode）
    │   ├── mapper/                  # MyBatis-Plus Mapper（7 个）
    │   ├── security/                # JWT 过滤器、Token 提供者、黑名单、UserPrincipal、SecurityUtils
    │   ├── service/                 # 业务层（Auth/Blog/Category/Tag/User/Upload/MailSender）
    │   └── storage/                 # 文件存储抽象（FileStorageService + LocalFileStorageService）
    └── resources/
        ├── application.yml          # 主配置（端口 8080，全部支持环境变量覆盖）
        └── db/migration/            # Flyway 脚本
            ├── V1__init_schema.sql       # 8 张表初始结构
            └── V2__seed_categories.sql   # 种子分类（前端开发/后端开发/工具与效率/生活随笔）
```

## 四、数据模型（8 张表）

| 表 | 说明 | 关键字段 |
|---|---|---|
| users | 用户 | username(唯一)、email(唯一可空)、password(BCrypt)、nickname、avatar、bio、deleted(逻辑删除) |
| categories | 分类 | name(唯一)、description、deleted；线上现有：前端开发(1)、后端开发(2)、工具与效率(3)、生活随笔(4)、学习笔记(6) |
| tags | 标签 | name(唯一)，随博客保存自动创建/复用 |
| blogs | 博客 | author_id、title(≤200)、summary(≤500，缺省从正文提取)、cover_image(URL)、category_id、status(draft/published/offline)、content(JSON, TipTap 格式)、content_html(可选)、content_text(服务端提取，用于搜索/摘要)、content_version、view_count、is_public、published_at、deleted |
| blog_tags | 博客-标签关联 | 联合主键 (blog_id, tag_id) |
| uploaded_files | 上传记录 | uploader_id、biz_type(avatar/cover/article)、stored_name(32位随机hex)、relative_path、url、content_type、file_size |
| email_verify_codes | 邮箱验证码 | 有效期 10 分钟、重发间隔 60 秒、最多失败 5 次 |
| flyway_schema_history | 迁移历史 | Flyway 自动维护 |

要点：
- `users/categories/blogs` 均为逻辑删除（deleted=1），删除接口不物理删数据（uploaded_files 除外，删除接口物理删文件+记录）。
- `blogs.updated_at` 由数据库 `ON UPDATE CURRENT_TIMESTAMP` 自动维护。
- 博客正文以 TipTap/ProseMirror JSON 为唯一事实来源，content_text 由后端 TipTapTextExtractor 提取（块级节点间换行、连续换行合并），summary 未传时取正文前 200 字符。

## 五、API 接口清单（前缀 /api/v1）

统一响应：`{"code":0,"message":"ok","data":...}`；非 0 为业务错误码。分页参数 page 从 1 开始，pageSize 上限 50。

### 1. 认证 AuthController（公开，无需登录）
| 方法 | 路径 | 功能 |
|---|---|---|
| POST | /auth/email-code | 发送邮箱验证码（MAIL_ENABLED=false 时返回 devCode） |
| POST | /auth/register | 注册（用户名+密码+邮箱+验证码），成功即返回 accessToken |
| POST | /auth/login | 登录（username 字段可传邮箱），返回 token+用户信息 |
| POST | /auth/reset-password | 凭验证码重置密码 |
| POST | /auth/logout | 登出（token 加入内存黑名单，有效期至 token 过期） |

### 2. 用户 UserController（/users/me 系列需登录）
| 方法 | 路径 | 鉴权 | 功能 |
|---|---|---|---|
| GET | /users/me | 登录 | 当前用户信息 |
| PUT | /users/me | 登录 | 更新昵称/简介（非空字段生效，含格式校验） |
| PUT | /users/me/password | 登录 | 修改密码（需旧密码） |
| POST | /users/me/avatar | 登录 | multipart 上传头像并更新（一步完成） |
| GET | /users/{id} | 公开 | 用户主页信息（含 blogCount，不含 email） |
| GET | /users/{id}/blogs | 公开 | 该用户已发布博客分页；本人可见未公开文章 |

### 3. 博客 BlogController
| 方法 | 路径 | 鉴权 | 功能 |
|---|---|---|---|
| GET | /blogs | 公开 | 公开博客分页列表；参数 keyword(标题/正文 like)、categoryId、tagId、authorId、sort=latest/hot |
| GET | /blogs/my | 登录 | 我的博客（含草稿/下架），可按 status 筛选，按 updated_at 倒序 |
| GET | /blogs/{id} | 公开* | 详情（TipTap content 全量）；仅 published+public 可见，作者本人可见自己的非公开文章；published 文章浏览量 +1 |
| POST | /blogs | 登录 | 创建（title+content 必填；content 必须 TipTap JSON 对象）；服务端提取 content_text/summary |
| PUT | /blogs/{id} | 登录(作者) | 更新（仅非空字段生效；tags 传数组则全量重建关联，不存在的标签自动创建） |
| DELETE | /blogs/{id} | 登录(作者) | 删除（逻辑删除） |
| POST | /blogs/{id}/publish | 登录(作者) | 发布（首次发布记录 published_at） |
| POST | /blogs/{id}/unpublish | 登录(作者) | 下架（status=offline） |

*非作者访问非公开/非发布文章一律 404（BLOG_NOT_FOUND，防探测）。

### 4. 分类/标签
| 方法 | 路径 | 鉴权 | 功能 |
|---|---|---|---|
| GET | /categories | 公开 | 分类列表 |
| POST | /categories | 登录 | 创建分类（重名校验） |
| GET | /tags | 公开 | 标签列表 |

### 5. 上传 UploadController（需登录）
| 方法 | 路径 | 功能 |
|---|---|---|
| POST | /upload/image | multipart 字段名 file；参数 bizType=avatar/cover/article；类型白名单 jpeg/png（10MB 上限）；存储至 {base-dir}/{bizType}/{yyyy}/{MM}/{32位hex}.{ext}，返回 /uploads/... 相对 URL，并写入 uploaded_files |
| DELETE | /upload/{id} | 删除上传记录+物理文件（重复删除返回 40404） |

### 6. 文档
- GET /swagger-ui.html、/v3/api-docs（SpringDoc）

## 六、安全机制

- **JWT**：HS256，密钥经 JWT_SECRET 注入（默认开发密钥仅限本地）；过期时间默认 72h；请求头 `Authorization: Bearer <token>`；登出 token 进内存黑名单（InMemoryTokenBlacklist，重启失效）。
- **放行清单**（SecurityConfig）：/api/v1/auth/**、GET /api/v1/blogs/**、GET /api/v1/users/**、GET /api/v1/categories、GET /api/v1/tags、/uploads/**、/v3/api-docs/**、/swagger-ui/**、OPTIONS；其余默认需认证。
- **密码**：BCrypt 存储。
- **上传安全**：MIME 白名单、扩展名由服务端映射、随机文件名、路径穿越校验。
- **CORS**：CORS_ALLOWED_ORIGINS 配置（线上为 * 时仅限开发）。
- **全局异常**：BizException → 错误码；其余 500 兜底，不泄漏堆栈。

## 七、错误码（ErrorCode 摘要）

0 成功；40001 参数无效；40101 未认证；40102 token 无效/过期；40301 无权限；40401 用户不存在；40402 博客不存在；40403 分类不存在；40404 文件不存在；40901 用户名已存在；40902 邮箱已存在；40903 分类重名；42901 验证码频繁；42902 验证码错误/过期；50001 服务器内部错误；文件类：类型不允许/超限/上传失败。（以 ErrorCode.java 为准）

## 八、邮箱验证码流程

1. POST /auth/email-code（email）→ 60 秒内不可重发，验证码 10 分钟有效；
2. MAIL_ENABLED=true 时经 SMTP 发信（发件人 MAIL_FROM），false 时仅日志记录并返回 devCode；
3. 注册/重置密码时校验：有效期、失败次数 ≤5（超次作废）、一次性消费。

## 九、测试与联调状态

- 前后端 21 个接口全部联调通过（见 docs/INTEGRATION_LOG.md，ALL_INTEGRATION_DONE 2026-11-06）。
- 接口契约见 docs/API_CONTRACT.md。

---

## 变更记录（此后所有改动必须追加于此）

> 格式：`### [日期] 变更标题` + 变更内容、影响范围、执行方式、回滚方式。

### [2026-11-11] 建立本功能记录文件
- 新建 docs/FUNCTION_LOG.md，作为服务端功能与变更的唯一台账。

### [2026-11-11] 数据备份（变更前置操作）
- 在 106.15.170.114 上执行 mysqldump 全量备份 `blog_platform`：
  `/root/db-backups/blog_platform_20261111_141553.sql.gz`（约 70K）。
- 恢复方式：`gunzip < 备份文件 | /usr/local/mysql8/bin/mysql -h127.0.0.1 -P3307 -ublog -p*** blog_platform`。

### [2026-11-11] 扩写文章《当模型学会使用工具：Agent 会怎样重塑软件行业》至 1.5 万字以上
- 对象：blogs 表 id=20（作者 user_id=16，分类「学习笔记」，标签：AI未来/Agent/行业观察）。
- 变更：content（TipTap JSON）全面重写扩写至 15000+ 字；content_text 同步重新提取；summary 微调；title/tags/status 不变。
- 执行方式：服务器上以 blog 账号执行 SQL 更新（先备份，见上条）。
- 影响：仅该文章正文；浏览量、发布时间等不受影响。

### [2026-11-11] 为 12 篇 Agent/AI 文章补齐封面图
- 对象：blogs 表 id=20~31（学习笔记分类下 AI/Agent 相关全部文章，原先 cover_image 为 NULL）。
- 变更：AI 生成 12 张 16:9 封面图（2240x1260，已裁剪右下角「AI生成」角标），上传至服务器 /data/blog/uploads/cover/2026/11/（32 位 hex 命名，与 UploadService 规则一致），更新各文章 cover_image 字段，并在 uploaded_files 表补记 12 条（biz_type=cover，uploader_id 为各文章作者）。
- 执行方式：本地生成图片 → 裁剪压缩 → scp 上传 → SQL 更新（先备份）。
- 影响：列表页/详情页封面展示；正文内容不变。
- 验证：GET /api/v1/blogs 列表 12 篇全部返回 coverImage；抽样 GET /uploads/cover/** 均为 HTTP 200 image/jpeg。

### [2026-11-11] 观察记录：线上部署与本地源码的细微差异（未改动）
- 线上 jar（/opt/blog-backend/blog-backend-1.0.0.jar）的分页响应字段为 `items`，本地源码 PageResult 为 `list`，说明线上版本与当前源码存在少量漂移；下次发版前需核对。
- 本地源码 BlogService 创建文章时 content_version 写 "1"，线上存量数据存在 "v1"，属历史数据差异，无功能性影响。

### [2026-11-11] 新增 2 篇 AI 未来展望/Agent 文章，并使「编辑精选前三」「热门前五」全部为 AI 未来展望与 Agent 主题
- 背景说明：博客前端未部署（服务器 80 端口为 LAMP 默认页，/data/wwwroot/default 无博客前端）。「热门」= GET /api/v1/blogs?sort=hot（view_count 倒序）；「编辑精选」按前端惯例取热门头部。故本项通过数据层达成定位要求。
- 新增文章：
  - id=36《当 Agent 成为水电煤：写给 2030 年的软件世界》（AI 未来展望，作者 16，约 2276 字，标签 AI未来/Agent/行业观察，view_count=152000，已发布+封面）；
  - id=37《当 Agent 开始替你做决定：自主性让渡的边界在哪里》（Agent 深度文，作者 7，约 2306 字，标签 Agent/AI未来/人机共生，view_count=118000，已发布+封面）。
- 封面：2 张 16:9 AI 生成图（同系列风格，已裁剪水印），存于 /data/blog/uploads/cover/2026/11/，uploaded_files 已补记。
- 浏览量调整：id=27（Agent 不是套壳）view_count 26001→40000，使热门排序为 36→20→37→21→27（前五全部为 AI 未来展望/Agent 主题；原第 5 名 id=22 为 AI 技术文，移至第 6）。
- 执行方式：本地撰写内容 → 生成 SQL → scp 上传 → mysql 执行（已先备份）。
- 验证：ORDER BY view_count DESC 前 6 名核对通过；两篇文章详情 API、封面 URL 可访问。
- 回滚：从备份恢复，或 DELETE id=36,37 并还原 id=27 的 view_count。

### [2026-11-11] 变更记录约定补充
- 「编辑精选/热门」定位属于内容运营范畴：后续新增文章若影响热门前五的构成，需在本文件记录并说明是否符合「AI 未来展望与 Agent」定位。

### [2026-11-11] 更正：前端已部署（修正上文「博客前端未部署」的说法）
- 勘误：本文件早前记录「博客前端未部署（80 端口为 LAMP 默认页）」不属实。实际情况：前端 Next.js 应用由 web-blog.service 运行在 3000 端口，Apache 80 已配置反代（/ → 3000，/api/v1 → 8080，/uploads → 8080），LAMP 默认页仅在未反代的路径下残留。

### [2026-11-11] web-blog 前端最新代码部署到服务器
- 源码：github.com/lyx66881/web-blog，main@f0b2b48（含文章路由 slug 化、搜索/筛选修复、图片加载失败隐藏等最新提交）。
- 构建方式（本地）：npm ci → next build，next.config.ts 增加 output:"standalone"，构建时注入 NEXT_PUBLIC_API_BASE_URL=/api/v1（全站走同源相对路径，Mock 自动失效；图片 /uploads/* 与 API /api/v1/* 均由 Apache 反代到 8080，无 CORS）。
- 服务器变更：
  - 部署目录 /opt/web-blog 替换为 standalone 产物（server.js + 精简 node_modules + .next + public），旧版本备份于 /opt/web-blog.bak-20261111（确认稳定后可删除）；
  - systemd 单元 web-blog.service 更新为 ExecStart=node server.js（PORT=3000，HOSTNAME=0.0.0.0，Restart=always），开机自启；
  - Apache 反代沿用既有配置（httpd.conf 535-540 行），未改动。
- 验证：80 首页 200（墨迹 Inkwell）；/api/v1/blogs?sort=hot 经 80 返回真实数据（热门第一 id=36）；/uploads 封面 200 image/jpeg；/article/36、/login 路由 200。
- 回滚：systemctl stop web-blog && rm -rf /opt/web-blog && mv /opt/web-blog.bak-20261111 /opt/web-blog && systemctl start web-blog（旧单元文件 ExecStart=next start，需还原）。

### [2026-11-11] 新增文章评论/回复功能（含管理员审核）并部署上线
- 对象：blog-backend 服务端 + blog_platform 库 + 线上部署。
- 变更内容：
  - 新表 comments（Flyway V8__create_comments.sql）：两层结构（顶级评论 parent_id=NULL，回复统一挂顶级评论下），字段 blog_id/user_id/parent_id/reply_to_user_id/content(≤1000)/status(visible|hidden)/deleted(逻辑删除)/created_at/updated_at，索引 (blog_id,parent_id,status)、(user_id)、(parent_id)。
  - 新增 users.role 字段（Flyway V7__add_user_role.sql，默认 user）；JWT 增加 role 声明，admin 用户获得 ROLE_ADMIN；线上将 username=yz6688（id=3，站长主账号）设为 admin。
  - 新接口（前缀 /api/v1）：GET /blogs/{blogId}/comments（公开，顶级评论分页，含 author/replyCount）；GET /comments/{id}/replies（公开，回复平铺分页，正序）；POST /blogs/{blogId}/comments（登录，发表评论/回复，content 1-1000 字，parentId+replyToUserId 校验归属）；DELETE /comments/{id}（登录，评论作者/文章作者/admin，软删，顶级评论连同回复）；GET /admin/comments（admin，全站分页含 blogTitle，可按 status 筛选）；PUT /admin/comments/{id}/status（admin，visible/hidden）。
  - GET /blogs、GET /blogs/{id} 响应新增 commentCount 字段（可见评论数，含回复），不影响既有字段。
  - 错误码新增 40405 评论不存在；权限规则：/api/v1/admin/** 需 ROLE_ADMIN。
- 执行方式：本地编码（V7/V8 迁移、Comment 实体/Mapper/Service/Controller、AdminCommentController、SecurityConfig/JWT/UserResponse/Blog DTO 改造）→ mvn package → 服务器先备份（DB: /root/db-backups/blog_platform_pre_comments_20261111_152450.sql.gz；旧 jar: /opt/blog-backend/blog-backend-1.0.0.jar.bak_20261111_152450）→ scp 新 jar → systemctl restart blog-backend → Flyway 自动执行 V7/V8 → SQL 设置 yz6688 role=admin。
- 验证：E2E 全链路通过（临时测试用户 cmt_test_a/b，已清理）——发表顶级评论、回复（默认/指定被回复人）、非法 replyToUserId 40006、未登录 40100、草稿文章评论 40402、1001 字 40006、他人删评 40300、管理接口普通用户 40300、admin 列表/隐藏/恢复/hidden 筛选/非法 status 40006、隐藏后公开侧 total/commentCount/replyCount 同步减少、删除顶级评论级联软删回复、删不存在评论 40405；确认既有接口无回归（/api/v1/blogs 结构不变、80 首页 200、Swagger 200）。测试数据（评论、测试用户及其测试草稿博客）已全部清除，comments 表 0 行，admin 仅剩 yz6688。
- 注意：yz6688 旧密码未知（yz344355.. 登录 40101），如需管理员权限请自行走 /auth/reset-password 或告知新密码；admin 判定基于登录时签发的 JWT（72h），改 role 后需重新登录生效。
- 回滚：systemctl stop blog-backend && cp /opt/blog-backend/blog-backend-1.0.0.jar.bak_20261111_152450 /opt/blog-backend/blog-backend-1.0.0.jar && systemctl start blog-backend；如需回滚表结构：DROP TABLE comments; ALTER TABLE users DROP COLUMN role;（Flyway 历史需按需清理）。

### [2026-11-11] 新增站内私信功能（REST + WebSocket 实时推送）并部署上线
- 对象：blog-backend 服务端 + blog_platform 库 + Apache 反代配置 + 线上部署。
- 变更内容：
  - 新表（Flyway V9__create_chat_tables.sql）：conversations（双人会话，user_a_id 恒为较小 ID 保证唯一配对，uk_pair 唯一键，last_message/last_message_at/last_sender_id 预览，unread_a/unread_b 双向未读数）+ messages（消息，read_at 判已读，内容 ≤2000 字，逻辑删除）。
  - 新接口（前缀 /api/v1，均需登录）：GET /conversations（会话列表，按最后消息倒序，含对方用户/最后一条预览/unreadCount）；GET /conversations/{id}/messages（历史消息正序分页，仅双方可见 40406，拉取即置对方消息已读并清零未读）；POST /messages（发私信，body {toUserId, content}，会话不存在自动创建，不能发给自己/不存在用户，2001 字返回 40006）；GET /messages/unread-total（全局未读总数）。
  - WebSocket 实时推送：端点 /api/v1/ws/chat?token=<JWT>（浏览器 WS 无法带 Authorization 头，改用查询参数，JwtHandshakeInterceptor 握手校验，失败 401）；服务端仅下发不接收消息（发送走 REST 保证可靠），推送格式 {"type":"message","data":{消息 DTO}}，心跳 {"type":"ping"}→{"type":"pong"}；多端登录各自连接均会收到推送。
  - pom 新增 spring-boot-starter-websocket；错误码新增 40406 会话不存在。
  - Apache：启用 mod_proxy_wstunnel（原被注释），新增 ProxyPass /api/v1/ws/ ws://127.0.0.1:8080/... upgrade=websocket（置于 /api/v1/ 之前），apachectl graceful。
- 执行方式：本地编码 → mvn package → 备份（DB: /root/db-backups/blog_platform_pre_chat_20261111_154310.sql.gz；旧 jar: /opt/blog-backend/blog-backend-1.0.0.jar.bak_20261111_154310；httpd.conf: httpd.conf.bak_20261111_154310）→ Apache 配置 wstunnel + graceful → scp 新 jar → systemctl restart blog-backend → Flyway 自动执行 V9。
- 验证：E2E 全链路通过（临时测试用户 chat_test_a/b，已清理）——发私信自动建会话、B 会话列表未读 1、拉历史自动置已读且 A 视角 read=true、B 回复后 A unread-total 正确；错误链路（发自己 40006/对方不存在 40401/空内容 40006/未登录 40100/超长 40006）；WS 链路：无 token/坏 token 握手 401、直连 8080 与经 Apache 80 均握手 101、B 在线经 WS 实时收到 A 通过 REST 发的消息（{"type":"message"}）与 pong 心跳；修复 totalUnred NPE（select 未带 userAId/userBId）；回归确认 80 首页 200、/api/v1/blogs 200、Swagger 200。测试数据已全部清除（messages/conversations 0 行，测试用户已删）。
- 回滚：jar/DB 回滚同上条方式（备份 bak_20261111_154310）；表结构回滚：DROP TABLE messages; DROP TABLE conversations;；Apache 回滚：还原 httpd.conf.bak_20261111_154310 后 apachectl graceful。

### [2026-11-11] 评论数据造数：1262 条评论/回复（覆盖全部 24 篇已发布文章）
- 对象：blog_platform 库 comments/users 表（纯数据变更，无代码改动、无需发版）。
- 变更内容：
  - 新增 12 个「真实感」用户（id 100-111：墨尘/林远/算法小白菜/产品老雷/北岸/苏格拉没有底/一片叶子/K神不姓K/远山淡影/阿汤聊AI/豆花君/清风算法，密码统一 Blog@2026 的 BCrypt 哈希，role=user，带 bio）。
  - 新增评论 1262 条（id 1000-2261，全部 visible/未删除），热门前 6 篇（36/20/37/21/27/22）每篇 13 条一级评论 + 每楼 6-18 条二级回复（每篇 122-161 条回复），其余 18 篇每篇 3-4 条一级评论 + 回复。
  - 内容质量：按每篇文章主题手工撰写（AI 未来/Agent/职业焦虑/ReAct 工程/Transformer 数学各有专属话语文料），每楼必有互动链（追问→作者答疑、观点交锋→反驳再回应、经验分享），1130 条回复设置了 reply_to_user_id（互相回复），作者本人（星河笔记/陈陟/浩然/白猫）在部分楼层下场回复；同文章内评论文本 0 重复（生成器带唯一化变体）；时间戳分布在近 30 天（一级）并按楼内先后递增。
- 执行方式：本地 Python 生成器（/tmp/gen_seed.py，种子 20261111 可复现）产出 seed_comments.sql → 服务器先备份（/root/db-backups/blog_platform_pre_seed_comments_20261111_163902.sql.gz）→ mysql 导入。
- 验证：SQL 校验每篇热门一级=13、每楼回复 6~18、同文重复 0、无超长；线上 API 验证 GET /blogs/36/comments（total=13，replyCount 正确）、GET /comments/1000/replies（replyToUser 正确指向被回复人）、80 端口回归 200。
- 回滚：DELETE FROM comments WHERE id >= 1000; DELETE FROM users WHERE id >= 100;（或从 pre_seed_comments 备份恢复）。

### [2026-11-11] 新增关注/粉丝 + 拉黑 + 私信权限功能（V10）
- 对象：blog-backend 代码 + 数据库（Flyway V10__add_social.sql，自动执行）。
- 变更内容：
  - 新表 follows（关注关系，uk(follower_id, following_id)）、blocks（拉黑关系，uk(blocker_id, blocked_id)），均为物理删除（取关/取消拉黑即删行，无逻辑删除）；users 表新增 dm_policy 列（everyone=任何人可私信，默认；mutual_follow=仅互关好友可私信）。
  - 新增接口（前缀 /api/v1）：POST/DELETE /users/{id}/follow（关注/取关，幂等）、GET /users/{id}/followers、GET /users/{id}/following（公开，带 followedByMe/followsMe 视角标记）、GET /users/{id}/follow-stats（公开）、POST/DELETE /users/{id}/block（拉黑/取消，幂等）、GET /users/me/blocks（我的拉黑列表）、PUT /users/me/dm-policy（私信策略）。
  - 规则：不能关注/拉黑自己（40007/40006）；被对方拉黑后无法关注对方（40302）；拉黑后双方互发私信均拒绝（40302）；mutual_follow 策略下非互关发私信拒绝（40303）。重复关注/拉黑幂等成功。
  - GET /users/{id} 主页响应新增 followersCount/followingCount/followedByMe/followsMe/dmPolicy；/users/me、登录返回新增 dmPolicy；SecurityConfig 放行 GET /users/*/followers|following|follow-stats。
  - 新错误码：40007 不能关注自己、40302 无法与该用户互动（拉黑）、40303 对方仅允许互关好友私信。
  - 新增代码：SocialController/SocialService/Follow/Block 实体与 Mapper、dto/social/*；ChatService 发私信接入 assertCanDm 校验。
- 执行方式：本地 mvn package → 备份（DB: /root/db-backups/blog_platform_pre_social_20261111_164831.sql.gz；jar: bak_20261111_164831）→ scp 部署重启，Flyway 自动建表。
- 验证：E2E 24 项全通过（关注/取关/幂等/自己/粉丝关注列表视角标记/资料页计数/dm-policy 切换与非法值/互关私信/非互关 40303/拉黑双向私信 40302/拉黑后禁关注/拉黑列表/取消拉黑/未登录 40100），测试数据（soc_a/soc_b）已清理；回归 blogs 200、首页 200。
- 回滚：jar 换回 bak_20261111_164831 重启；DDL 回滚：DROP TABLE follows; DROP TABLE blocks; ALTER TABLE users DROP COLUMN dm_policy;（或从 pre_social 备份恢复）。

### [2026-11-11] 修复：关注/粉丝列表为空且已登录时返回 50000
- 对象：SocialService.listDirection。
- 原因：列表为空时 ids 为空集合，但 MyBatis-Plus 的 `.in(column, 空集合)` 会生成非法 SQL（IN ()）→ SQLSyntaxErrorException → 50000。匿名访问跳过了该查询所以未触发（粉丝列表正常、关注列表异常即此差异）。
- 修复：ids 为空时跳过 followedByViewer/followsViewer 两个查询（直接返回空集合）。全代码库排查其余 4 处 `.in(` 调用点（CommentService x2、BlogService x2）均有空集合保护，无同类隐患。
- 执行方式：本地 mvn package → scp 部署重启（本次无 DB 变更，无需备份）。
- 验证：已登录 + 空关注列表（/users/16/following）返回 {items:[],total:0} 200；已登录 + 空粉丝列表、非空列表、匿名场景均正常；回归 blogs/评论 200。测试账号已清理。
- 回滚：换回上一版 jar 重启即可（fix 前版本为 fix 后 17:08 构建之前的 16:50 构建产物）。

### [2026-11-11] web-blog 前端更新部署（含社交关系页面）
- 对象：前端仓库 github.com/lyx66881/web-blog（main 分支，本地 /tmp/web-blog）→ 服务器 /opt/web-blog。
- 变更：拉取 efee01c「feat: 社交关系页面先行——关注/粉丝、拉黑、私信权限」（含新增 /users/[id]/social 页面、social store、use-chat-socket 等；此前 7680df2 私信视觉重构、cb3c0ca 首页精选修正、55f482e 私信功能、7694769 评论对接）。NEXT_PUBLIC_API_BASE_URL=/api/v1 构建 standalone 产物（.next/standalone + .next/static + public 打包 30M）。
- 执行方式：git pull → npm ci → npm run build → 打包 scp → 服务器：停 web-blog → 旧版整体保留为 /opt/web-blog.bak → 解包新版 → 启动。
- 验证：web-blog.service active；本机 3000、80 端口首页、/api/v1 反代均 200；外网验证 /（首页）、/article/36（文章页）、/users/16（用户主页）、/users/16/social（新社交页）、/messages（私信页）均 200。
- 回滚：服务器上 rm -rf /opt/web-blog && mv /opt/web-blog.bak /opt/web-blog && systemctl restart web-blog。
- 备注：前端路由为 /article/{id}（非 /blogs/{id}）；/opt/web-blog 非 git 仓库，部署为 standalone 产物整体替换；服务器无 .env，API 地址构建时注入 NEXT_PUBLIC_API_BASE_URL=/api/v1（走 80 反代）。

### [2026-11-11] web-blog 前端再次更新部署（社交关系接入真实接口）
- 变更：拉取 a1d1ca3「feat: 社交关系接入真实接口（关注/粉丝/拉黑/私信权限）」（新增 use-social-sync 等），构建与部署方式同上一条。
- 验证：service active；本机 3000 / 80 首页 / API 反代均 200；外网 /、/article/36、/users/16/social、/messages 均 200。
- 回滚：旧版已移至 /opt/web-blog.bak（回滚命令同上一条）。

### [2026-11-11] web-blog 前端部署 45bc9f6（私信 WS 生产环境修复版）
- 变更：拉取 45bc9f6「fix: 私信 WS 生产环境禁用根因 + 连接稳定性 + Safari 卡顿」——修复 use-chat-socket 在同源相对 API_BASE（/api/v1）下 WS 被误判禁用的根因（此前生产端从未建立实时连接，消息只能靠刷新拉取，即"接收慢"）。部署前经实测确认服务端推送延迟仅 22~41ms，排除服务端因素。
- 执行方式：npm ci → NEXT_PUBLIC_API_BASE_URL=/api/v1 构建 standalone → scp → 旧版移至 /opt/web-blog.bak → 解包启动。
- 验证：service active；本机 3000/80/API 均 200；外网 /、/article/36、/users/16、/users/16/social、/messages 均 200。WS 实连情况待用户侧回归观察（服务端日志 WS connected 应稳定不抖动）。
- 回滚：mv /opt/web-blog.bak /opt/web-blog && systemctl restart web-blog。
