<template>
  <div class="page-enter">
    <div class="page-header">
      <div>
        <h2 class="page-title">用户管理</h2>
        <p class="page-desc">账号角色与封禁管控</p>
      </div>
    </div>

    <div class="panel">
      <div class="toolbar">
        <el-input v-model="query.keyword" placeholder="搜索用户名 / 昵称 / 邮箱" clearable style="width: 240px" @keyup.enter="onSearch" @clear="onSearch">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px" @change="onSearch">
          <el-option label="普通用户" value="user" />
          <el-option label="管理员" value="admin" />
        </el-select>
        <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="onSearch">
          <el-option label="正常" :value="0" />
          <el-option label="已封禁" :value="1" />
        </el-select>
        <el-button type="primary" plain @click="onSearch">查询</el-button>
        <el-button @click="onReset">重置</el-button>
        <span class="toolbar-hint">共 {{ total }} 位用户</span>
      </div>

      <el-table v-loading="loading" :data="items">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="用户" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="absoluteUrl(row.avatar)">{{ (row.nickname || row.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <div>
                <div class="user-name">{{ row.nickname || row.username }}</div>
                <div class="sub">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="邮箱" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.email || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="96">
          <template #default="{ row }">
            <el-tag :type="row.role === 'admin' ? 'danger' : 'info'" effect="light" round>{{ row.role === 'admin' ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="92">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'success' : 'error'" effect="light" round>{{ row.status === 0 ? '正常' : '已封禁' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="博客数" width="90">
          <template #default="{ row }">
            <el-link v-if="row.blogCount" type="primary" :underline="false" @click="goBlogs(row)">{{ row.blogCount }}</el-link>
            <span v-else class="muted">0</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <template v-if="row.id !== auth.user?.id">
              <el-button link type="primary" @click="goBlogs(row)">博客</el-button>
              <el-button link :type="row.status === 0 ? 'warning' : 'success'" @click="onToggleStatus(row)">
                {{ row.status === 0 ? '封禁' : '解封' }}
              </el-button>
              <el-button link type="primary" @click="onToggleRole(row)">
                {{ row.role === 'admin' ? '降为用户' : '设为管理员' }}
              </el-button>
              <el-button link type="danger" @click="openResetPassword(row)">重置密码</el-button>
            </template>
            <span v-else class="self-tag">当前账号</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        class="pagination-row"
        @size-change="load"
        @current-change="load"
      />
    </div>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="420px">
      <el-form label-width="80px">
        <el-form-item label="用户"> {{ resetTarget?.username }} </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="newPassword" placeholder="6-32 位" show-password />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="onResetPassword">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { apiAdminUsers, apiAdminUserStatus, apiAdminUserRole, apiAdminUserResetPassword } from '../api/admin'
import { useAuthStore } from '../stores/auth'
import type { AdminUserItem } from '../types/api'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const items = ref<AdminUserItem[]>([])
const total = ref(0)

const query = reactive({ page: 1, pageSize: 10, keyword: '', role: '', status: undefined as number | undefined })

const resetVisible = ref(false)
const resetTarget = ref<AdminUserItem | null>(null)
const newPassword = ref('')
const resetting = ref(false)

function formatTime(value?: string) {
  return value ? value.replace('T', ' ').slice(0, 16) : '-'
}

function absoluteUrl(url?: string) {
  if (!url || url.startsWith('http')) return url
  return `${location.origin}${url}`
}

/** 跳转博客管理页并按该作者筛选 */
function goBlogs(row: AdminUserItem) {
  router.push({ path: '/blogs', query: { authorId: String(row.id) } })
}

async function load() {
  loading.value = true
  try {
    const page = await apiAdminUsers({ ...query })
    items.value = page.items
    total.value = page.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  query.page = 1
  load()
}

function onReset() {
  query.keyword = ''
  query.role = ''
  query.status = undefined
  onSearch()
}

async function onToggleStatus(row: AdminUserItem) {
  const toBan = row.status === 0
  const action = toBan ? '封禁' : '解封'
  try {
    await ElMessageBox.confirm(`确定${action}用户「${row.username}」吗？${toBan ? '封禁后该用户将无法登录。' : ''}`, `${action}用户`, { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiAdminUserStatus(row.id, toBan ? 1 : 0)
    ElMessage.success(`已${action}`)
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

async function onToggleRole(row: AdminUserItem) {
  const toAdmin = row.role !== 'admin'
  const action = toAdmin ? '设为管理员' : '降为普通用户'
  try {
    await ElMessageBox.confirm(`确定将用户「${row.username}」${action}吗？`, '调整角色', { type: 'warning' })
  } catch {
    return
  }
  try {
    await apiAdminUserRole(row.id, toAdmin ? 'admin' : 'user')
    ElMessage.success('角色已更新')
    load()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  }
}

function openResetPassword(row: AdminUserItem) {
  resetTarget.value = row
  newPassword.value = ''
  resetVisible.value = true
}

async function onResetPassword() {
  if (!resetTarget.value) return
  if (newPassword.value.length < 6 || newPassword.value.length > 32) {
    ElMessage.warning('密码长度须为 6-32 位')
    return
  }
  resetting.value = true
  try {
    await apiAdminUserResetPassword(resetTarget.value.id, newPassword.value)
    ElMessage.success('密码已重置')
    resetVisible.value = false
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '操作失败')
  } finally {
    resetting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.user-name {
  font-weight: 500;
  color: var(--ink-900);
}
.sub {
  font-size: 12px;
  color: var(--ink-400);
}
.self-tag {
  font-size: 12px;
  color: var(--ink-400);
}
</style>
