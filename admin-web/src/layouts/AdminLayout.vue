<template>
  <el-container class="layout">
    <el-aside width="228px" class="aside">
      <div class="brand">
        <div class="brand-mark">墨</div>
        <div class="brand-text">
          <div class="brand-name">墨迹</div>
          <div class="brand-sub">内容管理台</div>
        </div>
      </div>
      <el-menu
        :default-active="route.path"
        router
        class="menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
      <div class="aside-foot">以墨为迹 · 落笔成章</div>
    </el-aside>

    <el-container>
      <el-header class="header">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item>管理台</el-breadcrumb-item>
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
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="password">
                  <el-icon><Key /></el-icon>修改密码
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
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

  <!-- 修改密码 -->
  <el-dialog v-model="pwdVisible" title="修改密码" width="440px" :close-on-click-modal="false">
    <el-alert
      v-if="!auth.user?.email"
      type="warning"
      :closable="false"
      show-icon
      title="当前账号未绑定邮箱"
      description="修改密码需要通过绑定邮箱接收验证码，请先在前台站点绑定邮箱后再试。"
      class="pwd-alert"
    />
    <template v-else>
      <div class="pwd-email">
        验证码将发送至绑定邮箱
        <span class="pwd-email-value">{{ auth.user.email }}</span>
      </div>
      <el-form label-width="90px" @keyup.enter="onChangePassword">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="当前使用的密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item label="邮箱验证码">
          <div class="pwd-code">
            <el-input v-model="pwdForm.emailCode" maxlength="6" placeholder="6 位数字" />
            <el-button :disabled="codeCountdown > 0" :loading="sendingCode" @click="onSendCode">
              {{ codeCountdown > 0 ? `${codeCountdown}s 后重发` : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </template>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button
        v-if="auth.user?.email"
        type="primary"
        :loading="changingPwd"
        :disabled="!pwdForm.oldPassword || !pwdForm.newPassword || !pwdForm.emailCode"
        @click="onChangePassword"
      >
        确认修改
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '../stores/auth'
import { apiGetStats, apiSendEmailCode, apiChangeMyPassword } from '../api/admin'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const menus = [
  { path: '/dashboard', title: '数据看板', icon: 'Odometer' },
  { path: '/blogs', title: '博客管理', icon: 'Document' },
  { path: '/users', title: '用户管理', icon: 'User' },
  { path: '/chats', title: '聊天记录', icon: 'ChatLineRound' },
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

// ==================== 修改密码 ====================

const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '', emailCode: '' })
const sendingCode = ref(false)
const codeCountdown = ref(0)
const changingPwd = ref(false)
let countdownTimer: ReturnType<typeof setInterval> | null = null

function openPasswordDialog() {
  pwdForm.oldPassword = ''
  pwdForm.newPassword = ''
  pwdForm.confirm = ''
  pwdForm.emailCode = ''
  codeCountdown.value = 0
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  pwdVisible.value = true
}

function startCountdown() {
  codeCountdown.value = 60
  countdownTimer = setInterval(() => {
    codeCountdown.value -= 1
    if (codeCountdown.value <= 0 && countdownTimer) {
      clearInterval(countdownTimer)
      countdownTimer = null
    }
  }, 1000)
}

async function onSendCode() {
  if (!auth.user?.email) return
  sendingCode.value = true
  try {
    await apiSendEmailCode(auth.user.email, 'change')
    ElMessage.success('验证码已发送，请查收邮箱')
    startCountdown()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '验证码发送失败')
  } finally {
    sendingCode.value = false
  }
}

async function onChangePassword() {
  if (pwdForm.newPassword.length < 6 || pwdForm.newPassword.length > 32) {
    ElMessage.warning('新密码长度须为 6-32 位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  changingPwd.value = true
  try {
    await apiChangeMyPassword({
      oldPassword: pwdForm.oldPassword,
      newPassword: pwdForm.newPassword,
      emailCode: pwdForm.emailCode
    })
    pwdVisible.value = false
    ElMessageBox.alert('密码修改成功，请使用新密码重新登录。', '提示', { type: 'success' })
      .then(async () => {
        await auth.logout()
        router.replace('/login')
      })
      .catch(() => {})
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '修改失败')
  } finally {
    changingPwd.value = false
  }
}

// ==================== 下拉命令 ====================

function onCommand(command: string) {
  if (command === 'password') {
    openPasswordDialog()
  } else if (command === 'logout') {
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

/* ---------- 侧边栏：墨 ---------- */
.aside {
  background: linear-gradient(180deg, var(--ink-900) 0%, var(--ink-950) 100%);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.brand {
  height: 64px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}
.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  background: var(--primary);
  color: #fff;
  font-family: var(--font-display);
  font-size: 20px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 8px rgba(59, 91, 219, 0.45);
}
.brand-name {
  color: #fff;
  font-family: var(--font-display);
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 4px;
  line-height: 1.2;
}
.brand-sub {
  color: rgba(255, 255, 255, 0.4);
  font-size: 11px;
  letter-spacing: 2px;
  margin-top: 1px;
}
.menu {
  border-right: none;
  flex: 1;
  background: transparent;
  padding: 10px 0;
}
.menu :deep(.el-menu-item) {
  color: rgba(255, 255, 255, 0.62);
  height: 44px;
  margin: 2px 10px;
  border-radius: 8px;
  transition: all 0.2s;
}
.menu :deep(.el-menu-item:hover) {
  background: rgba(255, 255, 255, 0.06);
  color: #fff;
}
.menu :deep(.el-menu-item.is-active) {
  background: var(--primary);
  color: #fff;
  box-shadow: 0 2px 10px rgba(59, 91, 219, 0.4);
}
.aside-foot {
  padding: 16px 20px;
  color: rgba(255, 255, 255, 0.22);
  font-size: 11px;
  letter-spacing: 3px;
  font-family: var(--font-display);
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  text-align: center;
}

/* ---------- 顶栏：瓷 ---------- */
.header {
  background: var(--card);
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--ink-100);
  height: 56px;
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
  color: var(--ink-700);
  outline: none;
  padding: 4px 6px;
  border-radius: 8px;
  transition: background 0.2s;
}
.user-info:hover {
  background: var(--ink-50);
}
.username {
  font-size: 14px;
}
.user-arrow {
  font-size: 12px;
  color: var(--ink-400);
}

/* ---------- 主内容区 ---------- */
.main {
  padding: 20px 24px;
  overflow-y: auto;
  background: var(--porcelain);
}

/* ---------- 修改密码弹窗 ---------- */
.pwd-alert {
  margin-bottom: 4px;
}
.pwd-email {
  font-size: 13px;
  color: var(--ink-500);
  background: var(--ink-50);
  border-radius: 8px;
  padding: 8px 12px;
  margin-bottom: 16px;
}
.pwd-email-value {
  color: var(--primary-deep);
  font-weight: 600;
  margin-left: 4px;
}
.pwd-code {
  display: flex;
  gap: 8px;
  width: 100%;
}
.pwd-code .el-input {
  flex: 1;
}
</style>
