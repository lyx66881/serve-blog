<template>
  <el-container class="layout">
    <el-aside width="220px" class="aside">
      <div class="brand">
        <el-icon :size="22"><Platform /></el-icon>
        <span>博客后台管理</span>
      </div>
      <el-menu
        :default-active="route.path"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.65)"
        active-text-color="#ffffff"
        class="menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>后台管理</el-breadcrumb-item>
          <el-breadcrumb-item>{{ String(route.meta.title || '') }}</el-breadcrumb-item>
        </el-breadcrumb>
        <div class="header-right">
          <el-badge :value="pendingFeedbacks" :hidden="!pendingFeedbacks" class="feedback-badge">
            <el-tooltip content="待处理反馈" placement="bottom">
              <el-button text @click="router.push('/feedbacks')">
                <el-icon :size="18"><Bell /></el-icon>
              </el-button>
            </el-tooltip>
          </el-badge>
          <el-dropdown @command="onCommand">
            <span class="user-info">
              <el-avatar :size="30" :src="absoluteUrl(auth.user?.avatar)">{{ displayInitial }}</el-avatar>
              <span class="username">{{ auth.user?.nickname || auth.user?.username }}</span>
              <el-icon><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { apiGetStats } from '../api/admin'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/dashboard', title: '数据看板', icon: 'Odometer' },
  { path: '/blogs', title: '博客管理', icon: 'Document' },
  { path: '/users', title: '用户管理', icon: 'User' },
  { path: '/categories', title: '分类管理', icon: 'FolderOpened' },
  { path: '/tags', title: '标签管理', icon: 'CollectionTag' },
  { path: '/feedbacks', title: '意见反馈', icon: 'ChatDotRound' }
]

const pendingFeedbacks = ref(0)

const displayInitial = computed(() => (auth.user?.nickname || auth.user?.username || 'A').slice(0, 1).toUpperCase())

function absoluteUrl(url?: string) {
  if (!url || url.startsWith('http')) return url
  return `${location.origin}${url}`
}

async function refreshPendingFeedbacks() {
  try {
    const stats = await apiGetStats()
    pendingFeedbacks.value = stats.pendingFeedbacks
  } catch {
    // 看板接口失败不打扰用户
  }
}

function onCommand(command: string) {
  if (command === 'logout') {
    ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
      .then(async () => {
        await auth.logout()
        router.replace('/login')
      })
      .catch(() => {})
  }
}

onMounted(refreshPendingFeedbacks)
</script>

<style scoped>
.layout {
  height: 100vh;
}
.aside {
  background-color: #001529;
  display: flex;
  flex-direction: column;
}
.brand {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 1px;
}
.menu {
  border-right: none;
  flex: 1;
}
.menu :deep(.el-menu-item.is-active) {
  background-color: #1677ff;
}
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e8e8e8;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}
.feedback-badge :deep(.el-badge__content) {
  top: 6px;
}
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: #333;
  outline: none;
}
.username {
  font-size: 14px;
}
.main {
  padding: 16px;
  overflow-y: auto;
}
</style>
