# 博客平台

前后端分离博客平台 + 管理后台。

- `blog-backend/` — Java 后端（Spring Boot 3.x + MyBatis-Plus + MySQL 8 + Flyway + JWT）
- `web-blog/` — 博客前端（规划中）
- `admin-web/` — 管理后台前端（Vue 3 + Element Plus + ECharts）
- `docs/API_CONTRACT.md` — 前后端接口契约（**接口变更必须同步更新**）
- `docs/DATABASE.md` — 数据库设计文档

## 技术栈

Java 21 / Spring Boot 3.5 / Spring Security + JWT / MyBatis-Plus / MySQL 8 / Flyway / SpringDoc OpenAPI / 本地磁盘文件存储

## 快速开始

### 1. 环境要求

- JDK 21+、Maven 3.8+
- MySQL 8.0+

### 2. 初始化数据库

```sql
CREATE DATABASE IF NOT EXISTS blog_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'blog'@'%' IDENTIFIED BY 'blog123456';
GRANT ALL PRIVILEGES ON blog_platform.* TO 'blog'@'%';
FLUSH PRIVILEGES;
```

表结构和种子数据由 Flyway 在应用启动时自动创建，无需手工执行 SQL。

### 3. 配置

配置文件：[blog-backend/src/main/resources/application.yml](blog-backend/src/main/resources/application.yml)。
所有敏感/环境相关配置均支持环境变量覆盖：

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://127.0.0.1:3306/blog_platform?...` | 数据库连接 |
| `DB_USERNAME` | `blog` | 数据库用户 |
| `DB_PASSWORD` | `blog123456` | 数据库密码 |
| `JWT_SECRET` | 开发默认值 | JWT 签名密钥（HS512，**生产必须覆盖**，≥32 字节） |
| `JWT_EXPIRE_HOURS` | `72` | Token 有效期（小时） |
| `UPLOAD_BASE_DIR` | `/data/blog/uploads` | 上传文件根目录（勿放项目源码目录） |
| `CORS_ALLOWED_ORIGINS` | `*` | 允许的跨域来源，逗号分隔（生产改为前端域名） |

服务端口：`8080`（`server.port`）。

### 4. 启动

```bash
cd blog-backend
mvn spring-boot:run
# 或打包运行
mvn clean package -DskipTests
java -jar target/blog-backend-1.0.0.jar
```

### 5. 验证

- Swagger 文档：http://localhost:8080/swagger-ui.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs
- 健康检查（无需登录）：`curl http://localhost:8080/api/v1/categories`

## 文件上传

- 目录结构：`{UPLOAD_BASE_DIR}/{avatar|cover|article}/{yyyy}/{MM}/{uuid}.{ext}`
- 静态访问：`GET /uploads/**` 直接映射到上传根目录，如
  `http://localhost:8080/uploads/cover/2026/11/xxx.png`
- 限制：jpeg/png/gif/webp，单文件 ≤10MB（`spring.servlet.multipart.max-file-size`）
- 安全：存储文件名服务端随机生成、扩展名由 MIME 白名单映射、删除时路径穿越校验
- 记录：所有上传写入 `uploaded_files` 表
- 扩展：`FileStorageService` 为统一存储接口，未来切换 MinIO/OSS 只需新增实现类

## 项目结构

```
blog-backend/src/main/java/com/blog/
├── BlogApplication.java     # 启动类
├── common/                  # 统一响应/错误码/全局异常/工具
├── config/                  # Security/MyBatis-Plus/Web/OpenAPI/配置属性
├── security/                # JWT 过滤器/Token/黑名单/认证入口
├── controller/              # REST 接口层
├── service/                 # 业务逻辑层
├── mapper/                  # MyBatis-Plus 数据访问层
├── entity/                  # 数据库实体
├── dto/                     # 请求/响应对象
└── storage/                 # 文件存储接口与本地实现
```

## 安全要点

- 密码 BCrypt 加密；登录参数校验；Spring Security 无状态 JWT 认证
- 默认拒绝：未在白名单的接口均需登录（401）；资源级权限在 Service 层校验（非作者 403）
- 草稿/下架博客仅作者可见，非作者访问返回 404（不泄露存在性）
- 退出登录 token 进黑名单（内存实现，重启清空；引入 Redis 后可平滑替换）
