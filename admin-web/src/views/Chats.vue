<template>
  <div class="page-enter">
    <div class="page-header">
      <div>
        <h2 class="page-title">聊天记录</h2>
        <p class="page-desc">用户会话与消息记录 · 只读查看，不影响已读状态</p>
      </div>
      <div class="page-actions">
        <span v-if="selectedUser" class="readonly-tag"><el-icon><View /></el-icon>只读模式</span>
      </div>
    </div>

    <div class="panel chat-panel">
      <div class="chat-layout">
        <!-- 左栏：用户 -->
        <div class="col col-users">
          <div class="col-head">
            <el-input v-model="userKeyword" placeholder="搜索用户名 / 昵称 / 邮箱" clearable @keyup.enter="searchUsers" @clear="searchUsers">
              <template #prefix><el-icon><Search /></el-icon></template>
            </el-input>
          </div>
          <div class="col-body">
            <div
              v-for="u in users"
              :key="u.id"
              class="user-row"
              :class="{ active: selectedUser?.id === u.id }"
              @click="pickUser(u)"
            >
              <el-avatar :size="34" :src="absoluteUrl(u.avatar)">{{ (u.nickname || u.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <div class="user-row-text">
                <div class="user-row-name">{{ u.nickname || u.username }}</div>
                <div class="sub">@{{ u.username }}</div>
              </div>
            </div>
            <div v-if="usersLoaded && !users.length" class="empty-hint">没有匹配的用户</div>
          </div>
          <div class="col-foot">
            <el-pagination
              v-model:current-page="userPage"
              :page-size="userPageSize"
              :total="userTotal"
              :pager-count="5"
              small
              layout="prev, pager, next"
              @current-change="loadUsers"
            />
          </div>
        </div>

        <!-- 中栏：会话列表 -->
        <div class="col col-convs">
          <div class="col-head">
            <template v-if="selectedUser">
              <el-avatar :size="28" :src="absoluteUrl(selectedUser.avatar)">{{ (selectedUser.nickname || selectedUser.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <span class="col-head-title">{{ selectedUser.nickname || selectedUser.username }} 的会话</span>
            </template>
            <span v-else class="col-head-title muted">请选择左侧用户</span>
          </div>
          <div class="col-body" v-loading="convLoading">
            <template v-if="selectedUser">
              <div
                v-for="c in conversations"
                :key="c.id"
                class="conv-row"
                :class="{ active: selectedConv?.id === c.id }"
                @click="pickConversation(c)"
              >
                <el-avatar :size="36" :src="absoluteUrl(c.other?.avatar)">{{ (c.other?.nickname || c.other?.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
                <div class="conv-row-text">
                  <div class="conv-row-top">
                    <span class="conv-row-name">{{ c.other?.nickname || c.other?.username }}</span>
                    <span class="conv-row-time">{{ formatTime(c.lastMessage?.at) }}</span>
                  </div>
                  <div class="conv-row-preview">{{ previewText(c) }}</div>
                </div>
                <span v-if="c.unreadCount" class="unread-dot">{{ c.unreadCount > 99 ? '99+' : c.unreadCount }}</span>
              </div>
              <div v-if="!convLoading && !conversations.length" class="empty-hint">该用户暂无会话</div>
              <div v-if="conversations.length < convTotal" class="more-row">
                <el-button link type="primary" :loading="convLoading" @click="loadMoreConvs">加载更多会话</el-button>
              </div>
            </template>
            <div v-else class="empty-pane">
              <el-icon :size="40" class="empty-pane-icon"><User /></el-icon>
              <p>先在左侧选择要查看的用户</p>
            </div>
          </div>
        </div>

        <!-- 右栏：消息记录 -->
        <div class="col col-chat">
          <div class="col-head chat-head">
            <template v-if="selectedConv">
              <el-avatar :size="30" :src="absoluteUrl(selectedConv.other?.avatar)">{{ (selectedConv.other?.nickname || selectedConv.other?.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
              <span class="col-head-title">{{ selectedConv.other?.nickname || selectedConv.other?.username }}</span>
              <span class="chat-head-sub">与 {{ selectedUser?.nickname || selectedUser?.username }} 的私信</span>
            </template>
            <span v-else class="col-head-title muted">会话消息记录</span>
          </div>
          <div class="chat-body" v-loading="msgLoading">
            <template v-if="selectedConv">
              <div class="chat-scroll" ref="scrollRef">
                <div class="more-row more-older">
                  <el-button v-if="olderAvailable" link type="primary" :loading="msgLoading" @click="loadOlder">查看更早消息</el-button>
                  <span v-else-if="messages.length" class="muted">已到最早消息</span>
                </div>
                <div
                  v-for="m in messages"
                  :key="m.id"
                  class="msg-row"
                  :class="{ mine: m.sender?.id === selectedUser?.id }"
                >
                  <el-avatar :size="30" class="msg-avatar" :src="absoluteUrl(m.sender?.avatar)">{{ (m.sender?.nickname || m.sender?.username || 'U').slice(0, 1).toUpperCase() }}</el-avatar>
                  <div class="msg-content">
                    <div class="msg-meta">
                      <span class="msg-sender">{{ m.sender?.nickname || m.sender?.username }}</span>
                      <span class="msg-time">{{ formatTime(m.createdAt) }}</span>
                      <span v-if="m.msgType === 'image'" class="msg-type-tag">图片</span>
                    </div>
                    <div class="bubble">
                      <img
                        v-if="m.msgType === 'image' && m.imageUrl"
                        class="bubble-image"
                        :src="absoluteUrl(m.imageUrl)"
                        :style="imageStyle(m)"
                        @click="previewImage(absoluteUrl(m.imageUrl) || '')"
                      />
                      <span v-else class="bubble-text">{{ m.content }}</span>
                    </div>
                  </div>
                </div>
                <div v-if="!msgLoading && !messages.length" class="empty-hint">该会话暂无消息</div>
              </div>
            </template>
            <div v-else class="empty-pane">
              <el-icon :size="40" class="empty-pane-icon"><ChatLineRound /></el-icon>
              <p>选择中间的会话查看消息记录</p>
            </div>
          </div>
          <div class="chat-foot">
            <span v-if="selectedConv">只读查看 · 消息不会标记为已读</span>
            <span v-else>&nbsp;</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineRound, Search, User, View } from '@element-plus/icons-vue'
import { apiAdminUsers } from '../api/admin'
import { apiAdminUserConversations, apiAdminChatMessages } from '../api/chats'
import type { AdminConversationItem, AdminMessageItem, AdminUserItem } from '../types/api'

// ==================== 左栏：用户 ====================
const userKeyword = ref('')
const users = ref<AdminUserItem[]>([])
const userPage = ref(1)
const userPageSize = 20
const userTotal = ref(0)
const usersLoaded = ref(false)
const selectedUser = ref<AdminUserItem | null>(null)

function searchUsers() {
  userPage.value = 1
  loadUsers()
}

async function loadUsers() {
  try {
    const res = await apiAdminUsers({ page: userPage.value, pageSize: userPageSize, keyword: userKeyword.value || undefined })
    users.value = res.items
    userTotal.value = res.total
    usersLoaded.value = true
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '用户列表加载失败')
  }
}

// ==================== 中栏：会话 ====================
const conversations = ref<AdminConversationItem[]>([])
const convPage = ref(1)
const convPageSize = 20
const convTotal = ref(0)
const convLoading = ref(false)
const selectedConv = ref<AdminConversationItem | null>(null)

async function pickUser(u: AdminUserItem) {
  if (selectedUser.value?.id === u.id) return
  selectedUser.value = u
  selectedConv.value = null
  messages.value = []
  convPage.value = 1
  convTotal.value = 0
  await loadConversations()
}

async function loadConversations() {
  if (!selectedUser.value) return
  convLoading.value = true
  try {
    const res = await apiAdminUserConversations(selectedUser.value.id, { page: convPage.value, pageSize: convPageSize })
    conversations.value = res.items
    convTotal.value = res.total
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '会话列表加载失败')
  } finally {
    convLoading.value = false
  }
}

async function loadMoreConvs() {
  if (!selectedUser.value) return
  convPage.value += 1
  convLoading.value = true
  try {
    const res = await apiAdminUserConversations(selectedUser.value.id, { page: convPage.value, pageSize: convPageSize })
    conversations.value = [...conversations.value, ...res.items]
    convTotal.value = res.total
  } catch (error) {
    convPage.value -= 1
    ElMessage.error(error instanceof Error ? error.message : '会话加载失败')
  } finally {
    convLoading.value = false
  }
}

function previewText(c: AdminConversationItem) {
  if (!c.lastMessage?.content) return '[无消息]'
  return c.lastMessage.content
}

// ==================== 右栏：消息记录 ====================
const messages = ref<AdminMessageItem[]>([])
const msgPage = ref(1)
const msgPageSize = 50
const msgTotal = ref(0)
const msgLoading = ref(false)
const scrollRef = ref<HTMLElement | null>(null)

const olderAvailable = ref(false)

async function pickConversation(c: AdminConversationItem) {
  if (selectedConv.value?.id === c.id) return
  selectedConv.value = c
  msgPage.value = 1
  msgTotal.value = 0
  await loadMessages(true)
}

async function loadMessages(scrollBottom: boolean) {
  if (!selectedConv.value) return
  msgLoading.value = true
  try {
    const res = await apiAdminChatMessages(selectedConv.value.id, { page: msgPage.value, pageSize: msgPageSize })
    messages.value = res.items
    msgTotal.value = res.total
    olderAvailable.value = res.total > msgPage.value * msgPageSize
    if (scrollBottom) {
      await nextTick()
      scrollRef.value?.scrollTo({ top: scrollRef.value.scrollHeight })
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '消息记录加载失败')
  } finally {
    msgLoading.value = false
  }
}

async function loadOlder() {
  if (!selectedConv.value || !olderAvailable.value) return
  const prevHeight = scrollRef.value?.scrollHeight ?? 0
  msgPage.value += 1
  msgLoading.value = true
  try {
    const res = await apiAdminChatMessages(selectedConv.value.id, { page: msgPage.value, pageSize: msgPageSize })
    messages.value = [...res.items, ...messages.value]
    olderAvailable.value = res.total > msgPage.value * msgPageSize
    await nextTick()
    const delta = (scrollRef.value?.scrollHeight ?? 0) - prevHeight
    scrollRef.value?.scrollTo({ top: delta })
  } catch (error) {
    msgPage.value -= 1
    ElMessage.error(error instanceof Error ? error.message : '更早消息加载失败')
  } finally {
    msgLoading.value = false
  }
}

// ==================== 工具 ====================

function absoluteUrl(url?: string) {
  if (!url || url.startsWith('http')) return url
  return `${location.origin}${url}`
}

function formatTime(time?: string) {
  if (!time) return ''
  const d = new Date(time)
  if (Number.isNaN(d.getTime())) return time
  const now = new Date()
  const sameDay = d.toDateString() === now.toDateString()
  const sameYear = d.getFullYear() === now.getFullYear()
  const hh = `${d.getHours()}`.padStart(2, '0')
  const mm = `${d.getMinutes()}`.padStart(2, '0')
  if (sameDay) return `${hh}:${mm}`
  if (sameYear) return `${d.getMonth() + 1}-${d.getDate()} ${hh}:${mm}`
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()} ${hh}:${mm}`
}

function imageStyle(m: AdminMessageItem) {
  const maxW = 260
  const maxH = 200
  const w = m.imageWidth || maxW
  const h = m.imageHeight || maxH
  const scale = Math.min(1, maxW / w, maxH / h)
  return { width: `${Math.round(w * scale)}px`, height: `${Math.round(h * scale)}px` }
}

function previewImage(url: string) {
  window.open(url, '_blank')
}

onMounted(loadUsers)
</script>

<style scoped>
.chat-panel {
  padding: 0;
  overflow: hidden;
}
.chat-layout {
  display: flex;
  height: calc(100vh - 168px);
  min-height: 420px;
}

/* ---------- 三栏骨架 ---------- */
.col {
  display: flex;
  flex-direction: column;
  min-width: 0;
}
.col-users {
  width: 264px;
  flex-shrink: 0;
  border-right: 1px solid var(--ink-100);
}
.col-convs {
  width: 288px;
  flex-shrink: 0;
  border-right: 1px solid var(--ink-100);
  background: var(--ink-50);
}
.col-chat {
  flex: 1;
}
.col-head {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 0 14px;
  border-bottom: 1px solid var(--ink-100);
  background: var(--card);
}
.col-head-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.chat-head-sub {
  font-size: 12px;
  color: var(--ink-400);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.col-body {
  flex: 1;
  overflow-y: auto;
  position: relative;
}
.col-foot {
  flex-shrink: 0;
  border-top: 1px solid var(--ink-100);
  padding: 6px 10px;
  display: flex;
  justify-content: center;
}

/* ---------- 用户列表 ---------- */
.user-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.user-row:hover {
  background: var(--ink-50);
}
.user-row.active {
  background: var(--primary-soft);
  box-shadow: inset 3px 0 0 var(--primary);
}
.user-row-text {
  min-width: 0;
}
.user-row-name {
  font-size: 13px;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.sub {
  font-size: 11px;
  color: var(--ink-400);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ---------- 会话列表 ---------- */
.conv-row {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 14px;
  cursor: pointer;
  transition: background 0.15s;
}
.conv-row:hover {
  background: var(--ink-100);
}
.conv-row.active {
  background: var(--primary-soft);
  box-shadow: inset 3px 0 0 var(--primary);
}
.conv-row-text {
  flex: 1;
  min-width: 0;
}
.conv-row-top {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}
.conv-row-name {
  font-size: 13px;
  color: var(--ink-900);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.conv-row-time {
  font-size: 11px;
  color: var(--ink-400);
  flex-shrink: 0;
}
.conv-row-preview {
  font-size: 12px;
  color: var(--ink-500);
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.unread-dot {
  flex-shrink: 0;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--danger);
  color: #fff;
  font-size: 11px;
  line-height: 18px;
  text-align: center;
  font-variant-numeric: tabular-nums;
}

/* ---------- 消息流 ---------- */
.chat-body {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}
.chat-scroll {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px 8px;
}
.more-row {
  text-align: center;
  padding: 2px 0 10px;
}
.more-older {
  padding: 0 0 12px;
}
.msg-row {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}
.msg-row.mine {
  flex-direction: row-reverse;
}
.msg-avatar {
  flex-shrink: 0;
  margin-top: 18px;
}
.msg-content {
  max-width: 62%;
  min-width: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}
.msg-row.mine .msg-content {
  align-items: flex-end;
}
.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 4px;
  padding: 0 2px;
}
.msg-sender {
  font-size: 12px;
  color: var(--ink-700);
  font-weight: 600;
}
.msg-time {
  font-size: 11px;
  color: var(--ink-400);
  font-variant-numeric: tabular-nums;
}
.msg-type-tag {
  font-size: 10px;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border-radius: 4px;
  padding: 0 5px;
  line-height: 16px;
}
.bubble {
  border-radius: 10px;
  padding: 8px 12px;
  background: var(--ink-100);
  color: var(--ink-900);
  box-shadow: 0 1px 2px rgba(27, 32, 50, 0.05);
  max-width: 100%;
}
.msg-row.mine .bubble {
  background: var(--primary);
  color: #fff;
}
.bubble-text {
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.bubble-image {
  display: block;
  border-radius: 6px;
  cursor: zoom-in;
}
.readonly-tag {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  color: var(--primary-deep);
  background: var(--primary-soft);
  border: 1px solid var(--primary-line);
  border-radius: 20px;
  padding: 4px 12px;
}
.chat-foot {
  flex-shrink: 0;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-top: 1px solid var(--ink-100);
  font-size: 12px;
  color: var(--ink-400);
  background: var(--card);
}

/* ---------- 空态 ---------- */
.empty-hint {
  text-align: center;
  color: var(--ink-400);
  font-size: 13px;
  padding: 32px 0;
}
.empty-pane {
  height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: var(--ink-400);
  font-size: 13px;
}
.empty-pane-icon {
  color: var(--ink-200);
}
</style>
