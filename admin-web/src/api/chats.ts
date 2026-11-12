import { request } from './http'
import type { PageParams } from './admin'
import type { AdminConversationItem, AdminMessageItem, ApiPage } from '../types/api'

// ==================== 聊天记录管理（只读） ====================

/** 指定用户的会话列表（按最近消息时间倒序） */
export function apiAdminUserConversations(userId: number, params: PageParams) {
  return request<ApiPage<AdminConversationItem>>({
    method: 'get',
    url: `/admin/chats/users/${userId}/conversations`,
    params
  })
}

/** 会话消息记录（按时间正序，只读不标记已读） */
export function apiAdminChatMessages(conversationId: number, params: PageParams) {
  return request<ApiPage<AdminMessageItem>>({
    method: 'get',
    url: `/admin/chats/conversations/${conversationId}/messages`,
    params
  })
}
