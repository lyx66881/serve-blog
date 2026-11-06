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
| 1 | 认证 | POST /api/v1/auth/register | ⬜ 未联调 | - |
| 2 | 认证 | POST /api/v1/auth/login | ⬜ 未联调 | - |
| 3 | 认证 | POST /api/v1/auth/logout | ⬜ 未联调 | - |
| 4 | 用户 | GET /api/v1/users/me | ⬜ 未联调 | - |
| 5 | 用户 | PUT /api/v1/users/me | ⬜ 未联调 | - |
| 6 | 用户 | POST /api/v1/users/me/avatar | ⬜ 未联调 | - |
| 7 | 用户 | GET /api/v1/users/{id} | ⬜ 未联调 | - |
| 8 | 用户 | GET /api/v1/users/{id}/blogs | ⬜ 未联调 | - |
| 9 | 博客 | GET /api/v1/blogs（公开列表/搜索/筛选） | ⬜ 未联调 | - |
| 10 | 博客 | GET /api/v1/blogs/my | ⬜ 未联调 | - |
| 11 | 博客 | GET /api/v1/blogs/{id} | ⬜ 未联调 | - |
| 12 | 博客 | POST /api/v1/blogs | ⬜ 未联调 | - |
| 13 | 博客 | PUT /api/v1/blogs/{id} | ⬜ 未联调 | - |
| 14 | 博客 | DELETE /api/v1/blogs/{id} | ⬜ 未联调 | - |
| 15 | 博客 | POST /api/v1/blogs/{id}/publish | ⬜ 未联调 | - |
| 16 | 博客 | POST /api/v1/blogs/{id}/unpublish | ⬜ 未联调 | - |
| 17 | 上传 | POST /api/v1/upload/image | ⬜ 未联调 | - |
| 18 | 上传 | DELETE /api/v1/upload/{id} | ⬜ 未联调 | - |
| 19 | 分类标签 | GET /api/v1/tags | ⬜ 未联调 | - |
| 20 | 分类标签 | GET /api/v1/categories | ⬜ 未联调 | - |
| 21 | 分类标签 | POST /api/v1/categories | ⬜ 未联调 | - |

## 修复记录

| 日期 | Issue | 问题 | 处理 | commit |
|------|-------|------|------|--------|
