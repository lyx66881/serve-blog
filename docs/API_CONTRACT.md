# 博客平台 API 契约文档

> 前后端协同的唯一接口依据。接口变更必须同步更新本文档并知会对方，禁止未沟通擅自修改。
>
> 在线调试（Swagger UI）：`http://<host>:8080/swagger-ui.html`，OpenAPI JSON：`http://<host>:8080/v3/api-docs`

## 1. 通用约定

### 1.1 基础信息

| 项目 | 值 |
| --- | --- |
| Base URL | `http://<host>:8080/api/v1` |
| 数据格式 | `application/json; charset=UTF-8`（文件上传除外） |
| 时间格式 | ISO-8601 本地时间字符串，如 `2026-11-06T14:30:00` |
| 字段空值 | 响应中 `null` 字段**不输出**，前端用可选字段（`?`）处理 |

### 1.2 统一响应格式

所有接口（包括错误）返回 HTTP 200/4xx/5xx + 统一 JSON 结构：

```json
{
  "code": 0,        // 业务码，0 表示成功，非 0 见错误码表
  "message": "ok",  // 提示信息
  "data": { }       // 业务数据，失败时为 null（且不输出）
}
```

### 1.3 业务错误码

| code | HTTP | 含义 |
| --- | --- | --- |
| 0 | 200 | 成功 |
| 40000 | 400 | 请求参数错误 |
| 40001 | 400 | 不支持的文件类型 |
| 40004 | 400 | 文件大小超出限制（上限 10MB） |
| 40005 | 400 | 文件上传失败 |
| 40006 | 400 | 请求参数校验失败（message 含字段名） |
| 40100 | 401 | 未登录或登录已过期 |
| 40101 | 401 | 用户名或密码错误 |
| 40102 | 401 | 登录已过期，请重新登录 |
| 40103 | 401 | 无效的登录凭证 |
| 40104 | 400 | 原密码不正确 |
| 40105 | 400 | 验证码错误或已过期（连续错 5 次作废，需重新发送） |
| 40106 | 400 | 验证码发送过于频繁（60 秒内不可重复发送） |
| 40300 | 403 | 无权限执行此操作 |
| 40400 | 404 | 资源不存在 |
| 40401 | 404 | 用户不存在 |
| 40402 | 404 | 博客不存在（含草稿/下架被非作者访问、已删除） |
| 40403 | 404 | 分类不存在 |
| 40404 | 404 | 文件不存在 |
| 40500 | 405 | 请求方法不支持 |
| 40900 | 409 | 资源冲突（如分类名已存在） |
| 40901 | 409 | 用户名已被占用 |
| 40902 | 409 | 邮箱已被占用 |
| 50000 | 500 | 服务器内部错误 |

### 1.4 认证方式

- 机制：JWT（Bearer Token），算法 HS512，默认有效期 72 小时（服务端可配置）。
- 登录 / 注册成功后前端从 `data.accessToken` 取 token。
- 除「公开接口」外，所有请求需携带请求头：

```
Authorization: Bearer <accessToken>
```

- 退出登录后原 token 立即失效（服务端黑名单，重启后黑名单清空）。
- 收到 `code=40100/40102/40103` 时前端应清除本地 token 并跳转登录页。

**公开接口（无需登录）**：注册、登录、忘记密码、重置密码、公开博客列表/详情、用户主页及用户已发布博客、分类列表/详情、标签列表、上传文件静态资源、Swagger。

### 1.5 分页结构

分页响应统一包裹在 `data` 中：

```json
{
  "items": [ ... ],
  "total": 100,
  "page": 1,
  "pageSize": 10,
  "pages": 10
}
```

分页参数约定：`page` 从 1 开始，默认 1；`pageSize` 默认 10，最大 50（超出自动截断）。

### 1.6 博客正文 JSON 结构（TipTap / ProseMirror）

- **请求与响应均为 JSON 对象**（不是字符串），后端原样存储、原样返回，不修改结构。
- 顶层结构：`{"type": "doc", "content": [...]}`。
- 支持所有 TipTap 节点：段落、标题、代码块、表格、图片、链接、引用、列表、加粗/斜体/删除线/行内代码等 marks。
- 服务端基于该 JSON 自动提取纯文本（`content_text`）用于搜索与摘要，不影响正文存储。

```json
{
  "type": "doc",
  "content": [
    { "type": "heading", "attrs": { "level": 1 }, "content": [ { "type": "text", "text": "标题" } ] },
    { "type": "paragraph", "content": [
        { "type": "text", "text": "普通" },
        { "type": "text", "marks": [ { "type": "bold" } ], "text": "粗体" },
        { "type": "text", "marks": [ { "type": "link", "attrs": { "href": "https://example.com" } } ], "text": "链接" }
    ] },
    { "type": "image", "attrs": { "src": "/uploads/article/2026/11/xxx.png", "alt": "说明" } }
  ]
}
```

### 1.7 图片 URL 约定

- 上传接口返回相对 URL（如 `/uploads/article/2026/11/xxx.png`）。
- 前端访问时拼接后端 origin：`http://<host>:8080` + url。
- 博客 `coverImage`、用户 `avatar`、正文 `image.src` 均存该相对 URL。

---

## 2. 认证接口

### 2.1 注册 `POST /api/v1/auth/register`（公开）

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| username | string | 是 | 3-30 位字母/数字/下划线 |
| password | string | 是 | 6-32 位 |
| nickname | string | 否 | 最长 50，缺省取 username |
| email | string | 否 | 邮箱格式，唯一 |

响应 `data`：`TokenResponse`（结构见 2.3）。

错误：`40901` 用户名已占用、`40902` 邮箱已占用、`40006` 校验失败。

### 2.2 登录 `POST /api/v1/auth/login`（公开）

请求体：

```json
{ "username": "alice", "password": "secret123" }
```

> `username` 字段支持用户名或邮箱登录。

错误：`40101` 用户名或密码错误。

### 2.3 TokenResponse 结构

```json
{
  "accessToken": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 259200,
  "user": {
    "id": 1, "username": "alice", "nickname": "Alice",
    "email": "alice@test.com", "avatar": null, "bio": null,
    "createdAt": "2026-11-06T14:49:46"
  }
}
```

### 2.4 退出登录 `POST /api/v1/auth/logout`（需登录）

使当前 token 失效。响应：`{"code":0,"message":"ok"}`。

### 2.5 忘记密码 `POST /api/v1/auth/forgot-password`（公开）

请求体：

```json
{ "account": "alice 或 alice@test.com" }
```

> `account` 支持**用户名或邮箱**。

响应 `data`：

```json
{
  "sent": true,
  "devCode": "455788",
  "expiresInSeconds": 600,
  "resendIntervalSeconds": 60
}
```

| 字段 | 说明 |
| --- | --- |
| sent | 固定 `true` |
| devCode | 6 位验证码。**当前未接入邮件服务，为「模拟发送」：验证码直接随响应返回**；将来接入 SMTP 后此字段不再输出（接口契约不变），前端届时改为提示「验证码已发送至邮箱」 |
| expiresInSeconds | 验证码有效期（秒，当前 600 = 10 分钟） |
| resendIntervalSeconds | 可再次发送的最小间隔（秒，当前 60） |

前端流程建议：输入账号 → 调本接口 → 进入「输入验证码 + 新密码」步骤（开发阶段可将 `devCode` 展示出来或直接预填，方便联调）。

错误：`40401` 用户不存在、`40106` 发送过于频繁（60 秒内不可重复）、`40006` 校验失败。

### 2.6 重置密码 `POST /api/v1/auth/reset-password`（公开）

请求体：

```json
{ "account": "alice", "code": "455788", "newPassword": "newpass123" }
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| account | string | 是 | 用户名或邮箱，须与接收验证码的账号一致 |
| code | string | 是 | 6 位数字验证码 |
| newPassword | string | 是 | 6-32 位 |

响应：`{"code":0,"message":"ok"}`（`data` 为 null）。成功后用新密码重新登录。

错误：`40401` 用户不存在、`40105` 验证码错误或已过期、`40006` 校验失败。

> 防爆破：验证码**连续输错 5 次即作废**，需重新发送；验证码有效期 10 分钟，验证成功后立即失效。

---

## 3. 用户接口

### 3.1 获取当前登录用户 `GET /api/v1/users/me`（需登录）

响应 `data`：`UserResponse`（含 email）。

```json
{ "id": 1, "username": "alice", "nickname": "Alice", "email": "alice@test.com",
  "avatar": "/uploads/avatar/2026/11/xxx.png", "bio": "Java developer", "createdAt": "..." }
```

### 3.2 修改个人资料 `PUT /api/v1/users/me`（需登录，仅本人）

请求体（均可选，仅非空字段生效；username/email 不可修改）：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| nickname | string | 最长 50 |
| bio | string | 最长 500 |
| avatar | string | 头像 URL（由上传接口获得） |

响应 `data`：更新后的 `UserResponse`。

### 3.3 上传头像 `POST /api/v1/users/me/avatar`（需登录）

- 请求：`multipart/form-data`，字段名 `file`（图片，≤10MB）。
- 行为：等价于「上传图片(type=avatar) + 更新头像」，一步完成。
- 响应 `data`：更新后的 `UserResponse`。

### 3.4 用户公开主页 `GET /api/v1/users/{id}`（公开）

```json
{ "id": 1, "username": "alice", "nickname": "Alice",
  "avatar": "/uploads/avatar/...", "bio": "...",
  "createdAt": "2026-11-06T14:49:46", "blogCount": 1 }
```

错误：`40401` 用户不存在。注意：不含 email。

### 3.5 用户已发布博客 `GET /api/v1/users/{id}/blogs`（公开）

参数：`page`、`pageSize`。响应 `data`：分页结构，`items` 为 `BlogSummaryResponse[]`（仅已发布）。

### 3.6 修改密码 `PUT /api/v1/users/me/password`（需登录）

请求体：

```json
{ "oldPassword": "旧密码", "newPassword": "6-32 位新密码" }
```

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| oldPassword | string | 是 | 当前密码，校验错误返回 `40104` |
| newPassword | string | 是 | 6-32 位 |

响应：`{"code":0,"message":"ok"}`（`data` 为 null）。当前 token 修改后仍有效，前端建议引导用户重新登录。

错误：`40104` 原密码不正确、`40006` 校验失败。

---

## 4. 博客接口

### 4.1 博客状态

| 状态 | 含义 | 可见性 |
| --- | --- | --- |
| `draft` | 草稿 | 仅作者 |
| `published` | 已发布 | 所有人 |
| `offline` | 已下架 | 仅作者 |

公开接口（列表/详情）只暴露 `published`；非作者访问草稿/下架博客返回 `40402`（不泄露存在性）。

**公开性（`isPublic`）**：博客另有 `isPublic` 布尔字段（默认 `false`）。仅 `published` **且** `isPublic=true` 的博客参与公开列表（最新 / 热门 / 搜索 / 筛选）排名；`isPublic=false` 的已发布博客不出现在任何公开列表中，详情仅作者可见（他人访问 `40402`）。作者本人不受限制。创建/编辑时可设置。

### 4.2 公开博客列表 `GET /api/v1/blogs`（公开）

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| page / pageSize | long | 否 | 分页，默认 1 / 10 |
| keyword | string | 否 | 标题或正文关键词模糊搜索 |
| categoryId | long | 否 | 分类筛选 |
| tagId | long | 否 | 标签筛选（标签 ID 见 6.1） |
| authorId | long | 否 | 按作者筛选 |
| sort | string | 否 | `latest`（默认，按发布时间）/ `hot`（按浏览量） |

响应 `data`：分页结构，`items` 为 `BlogSummaryResponse[]`：

```json
{
  "id": 1,
  "author": { "id": 1, "username": "alice", "nickname": "Alice", "avatar": "..." },
  "title": "测试博客标题",
  "summary": "自动或自定义摘要",
  "coverImage": "/uploads/cover/...",
  "categoryId": 1, "categoryName": "前端开发",
  "tags": [ { "id": 1, "name": "Java" } ],
  "status": "published",
  "isPublic": true,
  "viewCount": 3,
  "publishedAt": "2026-11-06T14:53:19",
  "createdAt": "...", "updatedAt": "..."
}
```

### 4.3 我的博客列表 `GET /api/v1/blogs/my`（需登录）

参数：`status`（可选，`draft`/`published`/`offline`，缺省全部）、`page`、`pageSize`。
返回当前用户全部状态博客，按更新时间倒序。草稿查询即 `?status=draft`。

### 4.4 博客详情 `GET /api/v1/blogs/{id}`（公开，草稿/下架仅作者）

响应 `data`：`BlogResponse` = `BlogSummaryResponse` 全部字段 + 正文：

```json
{
  "id": 1, "author": { "...": "..." }, "title": "...", "summary": "...",
  "coverImage": "...", "categoryId": 1, "categoryName": "前端开发",
  "tags": [...], "status": "published",
  "isPublic": true,
  "content": { "type": "doc", "content": [ ... ] },
  "contentHtml": null,
  "contentVersion": "1",
  "viewCount": 4, "publishedAt": "...", "createdAt": "...", "updatedAt": "..."
}
```

| 字段 | 说明 |
| --- | --- |
| content | TipTap JSON **对象**，原样返回，阅读/编辑共用 |
| contentHtml | 正文 HTML（可选字段，前端创建/编辑时可提供；后端不生成） |
| contentText | 服务端内部用于搜索/摘要，**响应中不返回** |

访问已发布博客详情时浏览量 +1（所有访问均计数，含作者本人）。

### 4.5 创建博客 `POST /api/v1/blogs`（需登录）

请求体：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| title | string | 是 | 最长 200 |
| content | object | 是 | TipTap JSON 对象 |
| contentHtml | string | 否 | 正文 HTML |
| summary | string | 否 | 最长 500；缺省从正文提取前 200 字 |
| coverImage | string | 否 | 封面 URL（上传接口获得） |
| categoryId | long | 否 | 必须是已存在分类 |
| tags | string[] | 否 | 标签名数组，自动创建；≤10 个，每个 ≤30 字符 |
| isPublic | boolean | 否 | 是否公开，默认 `false`；公开才参与公开列表排名 |
| status | string | 否 | `draft`（默认）/ `published` |

响应 `data`：完整 `BlogResponse`。

### 4.6 编辑博客 `PUT /api/v1/blogs/{id}`（需登录，仅作者）

请求体同创建，**所有字段可选**，仅非空字段生效；`content` 变更时自动重新提取摘要。
状态变更不通过本接口（用 4.8 / 4.9）。`tags` 传 `[]` 清空，不传保持不变。
`isPublic` 传 `true`/`false` 可随时切换公开性；不传保持不变。

错误：`40300` 非作者、`40402` 博客不存在、`40403` 分类不存在。

### 4.7 删除博客 `DELETE /api/v1/blogs/{id}`（需登录，仅作者）

逻辑删除。响应：`{"code":0,"message":"ok"}`。

### 4.8 发布博客 `POST /api/v1/blogs/{id}/publish`（需登录，仅作者）

草稿/下架 → 已发布。首次发布记录 `publishedAt`，之后保持不变。
响应 `data`：更新后的 `BlogResponse`。

### 4.9 下架博客 `POST /api/v1/blogs/{id}/unpublish`（需登录，仅作者）

已发布 → 下架（不再公开可见，可重新发布）。
响应 `data`：更新后的 `BlogResponse`。

---

## 5. 文件上传接口

### 5.1 上传图片 `POST /api/v1/upload/image`（需登录）

请求：`multipart/form-data`

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| file | file | 是 | 图片文件：jpeg/png/gif/webp，≤10MB |
| type | string | 否 | `avatar` / `cover` / `article`（默认 `article`） |

响应 `data`：

```json
{
  "id": 1,
  "url": "/uploads/cover/2026/11/1f49521f5ef24118ba39f5343cf03078.png",
  "fileName": "test.png",
  "size": 70,
  "contentType": "image/png",
  "type": "cover"
}
```

错误：`40001` 类型不支持、`40004` 超大小限制。

### 5.2 删除已上传文件 `DELETE /api/v1/upload/{id}`（需登录，仅上传者）

同时删除磁盘文件与记录。**注意：文件被博客/头像引用时删除会导致图片失效，前端应避免。**
错误：`40404` 文件不存在、`40300` 非上传者。

---

## 6. 分类与标签接口

### 6.1 标签列表 `GET /api/v1/tags`（公开）

```json
{ "code": 0, "data": [ { "id": 1, "name": "Java" } ] }
```

**标签完全自定义**：创建/编辑博客时在 `tags` 数组中传入任意标签名（≤10 个，每个 ≤30 字符），服务端自动创建并沉淀到标签库，下次编辑文章时 `GET /tags` 即可看到。前端实现建议：标签选择器 = `GET /tags` 全量返回作为候选项 + 支持自由输入新标签（无需预先注册，保存文章时自动创建）。无独立创建/删除标签接口。

### 6.2 分类列表 `GET /api/v1/categories`（公开）

```json
{ "code": 0, "data": [ { "id": 6, "name": "学习笔记", "description": "语言语法、框架原理、开发踩坑等知识沉淀" } ] }
```

服务端预置 5 个默认分类：**前端开发(1)、后端开发(2)、工具与效率(3)、生活随笔(4)、学习笔记(6)**（id=5 已被历史数据占用）。

### 6.3 创建分类 `POST /api/v1/categories`（需登录）

请求体：`{ "name": "分类名（必填，≤50，唯一）", "description": "描述（≤200，可选）" }`。
错误：`40900` 分类名已存在。

> 编辑文章时如需新增自定义分类：先调本接口创建，再拿返回的 `id` 作为 `categoryId` 保存文章；标签无需此步骤（保存文章时自动创建）。

---

## 7. 联调备忘

1. 前端所有写接口请求体为 JSON（上传除外），勿用表单编码。
2. `content` 收发均为 JSON 对象；TipTap 编辑器 `editor.getJSON()` / `editor.commands.setContent()` 直接对接。
3. 分页参数用 query string，POST/PUT 用 JSON body。
4. 401 时统一清除 token 跳登录；403 提示无权限；40402 处理为「内容不存在或已下架」。
5. 静态图片可直接 `<img src>`（同源部署）或拼后端 origin（跨域部署，已开启 CORS）。
