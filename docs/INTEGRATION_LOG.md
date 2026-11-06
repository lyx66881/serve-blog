# 前后端联调进度账本（INTEGRATION_LOG）

> 本文件是前后端联调的**唯一进度判定依据**，由前端窗口维护勾选状态，后端窗口负责核对。
> 接口行为如有争议，以 [API_CONTRACT.md](./API_CONTRACT.md) 为准。

## 使用规则

1. **前端窗口**：每联调通过一个接口，把对应行的「状态」改为 `✅ 通过` 并 commit + push；联调失败时创建 Issue（见下方模板），把状态改为 `❌ 失败 #<Issue编号>`。
2. **后端窗口**：修复后在对应 Issue 中回复 `已修复，commit <hash>` 并关闭 Issue；每完成一轮修复，在本文件「修复记录」追加一行。
3. **完成信号**：全部接口状态为 `✅ 通过` 后，前端窗口在文件末尾追加一行 `ALL_INTEGRATION_DONE <日期>` 并 push。后端窗口看到此标记并抽查验证后，本次联调任务结束。
4. 前端窗口每次推送前先 `git pull --rebase origin main`，避免冲突；本文件冲突时以对方最新内容为基础合并。

## 接口联调清单

| # | 模块 | 接口 | 状态 | Issue |
|---|------|------|------|-------|
| 1 | 认证 | POST /api/v1/auth/register | ✅ 通过 | - |
| 2 | 认证 | POST /api/v1/auth/login | ✅ 通过 | - |
| 3 | 认证 | POST /api/v1/auth/logout | ✅ 通过 | - |
| 4 | 用户 | GET /api/v1/users/me | ✅ 通过 | - |
| 5 | 用户 | PUT /api/v1/users/me | ✅ 通过 | - |
| 6 | 用户 | POST /api/v1/users/me/avatar | ✅ 通过 | - |
| 7 | 用户 | GET /api/v1/users/{id} | ✅ 通过 | - |
| 8 | 用户 | GET /api/v1/users/{id}/blogs | ✅ 通过 | - |
| 9 | 博客 | GET /api/v1/blogs（公开列表/搜索/筛选） | ✅ 通过 | - |
| 10 | 博客 | GET /api/v1/blogs/my | ✅ 通过 | - |
| 11 | 博客 | GET /api/v1/blogs/{id} | ✅ 通过 | - |
| 12 | 博客 | POST /api/v1/blogs | ✅ 通过 | - |
| 13 | 博客 | PUT /api/v1/blogs/{id} | ✅ 通过 | - |
| 14 | 博客 | DELETE /api/v1/blogs/{id} | ✅ 通过 | - |
| 15 | 博客 | POST /api/v1/blogs/{id}/publish | ✅ 通过 | - |
| 16 | 博客 | POST /api/v1/blogs/{id}/unpublish | ✅ 通过 | - |
| 17 | 上传 | POST /api/v1/upload/image | ✅ 通过 | - |
| 18 | 上传 | DELETE /api/v1/upload/{id} | ✅ 通过 | - |
| 19 | 分类标签 | GET /api/v1/tags | ✅ 通过 | - |
| 20 | 分类标签 | GET /api/v1/categories | ✅ 通过 | - |
| 21 | 分类标签 | POST /api/v1/categories | ✅ 通过 | - |

## 联调验证说明（前端窗口，2026-11-06）

- 认证模块：注册即返回 accessToken（含 tokenType/expiresIn/user）；login 支持 username 字段传邮箱；logout 后旧 token 访问 users/me 返回 HTTP 401，前端已做登录态清除跳转。
- 用户模块：PUT /users/me 仅非空字段生效（bio/昵称验证）；POST /users/me/avatar 一步完成「上传 + 更新头像」并返回更新后的 user；GET /users/{id} 返回 blogCount 且不含 email，符合契约。
- 博客模块：content 为 TipTap JSON 对象直接传输（未 stringify）；创建 draft → publish → unpublish → delete 全链路验证；PUT 仅非空字段生效、tags 传数组自动创建；GET /blogs 验证 keyword/categoryId/tagId/sort=hot 筛选与 page/pageSize 分页（page 从 1 开始）。
- 上传模块：multipart 字段名 file，jpeg/png 验证通过，返回相对路径 /uploads/article/**，已验证可经后端 origin 直接访问；DELETE 首次成功、重复删除返回 40404。
- 分类标签：GET tags / GET categories 公开可访问；POST categories 创建「联调临时分类」成功，重复名校验符合契约。
- 浏览器端到端：首页列表/精选/分类、登录、详情页、TipTap 编辑器保存草稿（id=4）、草稿箱管理、用户主页 blogCount 均基于真实后端数据验证通过。

## 修复记录

| 日期 | Issue | 问题 | 处理 | commit |
|------|-------|------|------|--------|

## 后端抽查记录（后端窗口，2026-11-06）

收到 `ALL_INTEGRATION_DONE` 信号，完成独立抽查验证，结论：**联调真实有效，后端确认收工**。

- 数据库痕迹核对：新用户 `itliaohe06`（联调探针）真实注册；联调博客覆盖改名、下架（offline）、标签筛选（published）、草稿（draft）场景；新增 2 个分类；`uploaded_files` 表为空系前端测试后调用 DELETE 物理删除所致，磁盘残留 1 个测试文件（无害，后端清理）。
- 外网验证：`GET /api/v1/categories` HTTP 200；静态图片 `GET /uploads/avatar/**` HTTP 200（image/png）。
- 契约一致性：上传目录命名（avatar/article/cover）、响应包裹结构、错误码、分页参数与 `API_CONTRACT.md` 一致，无契约漂移。
- 后端侧待办（不影响联调结论）：清理磁盘残留测试文件、清理部署测试账号 `deploytest` 的联调数据。

ALL_INTEGRATION_DONE 2026-11-06（后端窗口已核验）

