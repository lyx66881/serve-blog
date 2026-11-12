<template>
  <div class="login-page">
    <!-- 左：品牌区（墨） -->
    <div class="brand-pane">
      <div class="ink-blot blot-1" />
      <div class="ink-blot blot-2" />
      <div class="vertical-motto">以墨为迹&nbsp;&nbsp;落笔成章</div>
      <div class="brand-body">
        <div class="brand-seal">墨</div>
        <h1 class="brand-title">墨迹</h1>
        <div class="brand-en">MOJI · BLOG PLATFORM</div>
        <p class="brand-desc">一笔一画，皆成文章。<br />这里是「墨迹」的内容中枢，管理你的文字疆域。</p>
      </div>
      <div class="brand-foot">© 2026 墨迹 · 内容管理平台</div>
    </div>

    <!-- 右：登录区（瓷） -->
    <div class="form-pane">
      <div class="form-box">
        <h2 class="form-title">欢迎回来</h2>
        <p class="form-sub">登录后台，继续管理你的内容</p>
        <el-form :model="form" size="large" @keyup.enter="onSubmit">
          <el-form-item>
            <el-input v-model="form.username" placeholder="管理员用户名" :prefix-icon="User" autocomplete="username" />
          </el-form-item>
          <el-form-item>
            <el-input v-model="form.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password autocomplete="current-password" />
          </el-form-item>
          <el-button type="primary" class="submit" size="large" :loading="loading" @click="onSubmit">
            进入管理台
          </el-button>
        </el-form>
        <div class="form-tip">仅管理员账号可登录后台</div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function onSubmit() {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(form.username.trim(), form.password)
    ElMessage.success('登录成功')
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/dashboard'
    router.replace(redirect)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
}

/* ---------- 左侧品牌区 ---------- */
.brand-pane {
  position: relative;
  flex: 1.15;
  background: linear-gradient(165deg, var(--ink-900) 0%, var(--ink-950) 70%);
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 0 8%;
  overflow: hidden;
}
.ink-blot {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  pointer-events: none;
}
.blot-1 {
  width: 420px;
  height: 420px;
  right: -120px;
  top: -140px;
  background: radial-gradient(circle at 40% 40%, rgba(59, 91, 219, 0.28), rgba(59, 91, 219, 0.06) 55%, transparent 70%);
}
.blot-2 {
  width: 300px;
  height: 300px;
  left: -80px;
  bottom: -100px;
  background: radial-gradient(circle at 55% 45%, rgba(255, 255, 255, 0.07), transparent 65%);
}
.vertical-motto {
  position: absolute;
  right: 7%;
  top: 50%;
  transform: translateY(-50%);
  writing-mode: vertical-rl;
  font-family: var(--font-display);
  font-size: 20px;
  letter-spacing: 14px;
  color: rgba(255, 255, 255, 0.16);
  user-select: none;
}
.brand-body {
  position: relative;
  z-index: 1;
}
.brand-seal {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  background: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: 30px;
  font-weight: 700;
  box-shadow: 0 6px 24px rgba(59, 91, 219, 0.5);
  margin-bottom: 26px;
}
.brand-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 46px;
  font-weight: 700;
  letter-spacing: 12px;
  line-height: 1.15;
}
.brand-en {
  margin-top: 10px;
  font-size: 12px;
  letter-spacing: 5px;
  color: rgba(255, 255, 255, 0.42);
}
.brand-desc {
  margin: 26px 0 0;
  font-size: 14px;
  line-height: 2;
  color: rgba(255, 255, 255, 0.55);
  border-left: 2px solid rgba(59, 91, 219, 0.65);
  padding-left: 14px;
}
.brand-foot {
  position: absolute;
  bottom: 26px;
  left: 8%;
  font-size: 12px;
  letter-spacing: 1px;
  color: rgba(255, 255, 255, 0.25);
}

/* ---------- 右侧登录区 ---------- */
.form-pane {
  flex: 1;
  background: var(--card);
  display: flex;
  align-items: center;
  justify-content: center;
}
.form-box {
  width: 340px;
  animation: page-enter 0.4s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}
.form-title {
  margin: 0;
  font-family: var(--font-display);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 2px;
  color: var(--ink-900);
}
.form-sub {
  margin: 8px 0 30px;
  font-size: 13px;
  color: var(--ink-500);
}
.submit {
  width: 100%;
  margin-top: 6px;
  letter-spacing: 6px;
}
.form-tip {
  margin-top: 18px;
  text-align: center;
  font-size: 12px;
  color: var(--ink-400);
}

/* ---------- 窄屏适配 ---------- */
@media (max-width: 768px) {
  .brand-pane {
    display: none;
  }
  .form-pane {
    background: var(--porcelain);
  }
  .form-box {
    background: var(--card);
    padding: 36px 30px;
    border-radius: var(--radius);
    box-shadow: var(--shadow-card);
  }
}
</style>
