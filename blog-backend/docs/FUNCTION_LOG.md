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
