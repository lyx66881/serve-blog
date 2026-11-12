import { request } from './http'
import type {
  AdminStats,
  AdminTagItem,
  AdminUserItem,
  ApiPage,
  BlogDetail,
  BlogSummary,
  CategoryItem,
  FeedbackItem,
  LoginResult
} from '../types/api'

export interface PageParams {
  page?: number
  pageSize?: number
  [key: string]: unknown
}

// ==================== 认证 ====================

export function apiLogin(username: string, password: string) {
  return request<LoginResult>({ method: 'post', url: '/auth/login', data: { username, password } })
}

export function apiLogout() {
  return request<void>({ method: 'post', url: '/auth/logout' })
}

// ==================== 数据看板 ====================

export function apiGetStats() {
  return request<AdminStats>({ method: 'get', url: '/admin/stats' })
}

// ==================== 博客管理 ====================

export function apiAdminBlogs(params: PageParams) {
  return request<ApiPage<BlogSummary>>({ method: 'get', url: '/admin/blogs', params })
}

export function apiAdminBlogDetail(id: number) {
  return request<BlogDetail>({ method: 'get', url: `/admin/blogs/${id}` })
}

export function apiAdminBlogStatus(id: number, status: string) {
  return request<BlogDetail>({ method: 'put', url: `/admin/blogs/${id}/status`, data: { status } })
}

export function apiAdminBlogDelete(id: number) {
  return request<void>({ method: 'delete', url: `/admin/blogs/${id}` })
}

// ==================== 用户管理 ====================

export function apiAdminUsers(params: PageParams) {
  return request<ApiPage<AdminUserItem>>({ method: 'get', url: '/admin/users', params })
}

export function apiAdminUserStatus(id: number, status: 0 | 1) {
  return request<AdminUserItem>({ method: 'put', url: `/admin/users/${id}/status`, data: { status } })
}

export function apiAdminUserRole(id: number, role: 'user' | 'admin') {
  return request<AdminUserItem>({ method: 'put', url: `/admin/users/${id}/role`, data: { role } })
}

export function apiAdminUserResetPassword(id: number, newPassword: string) {
  return request<void>({ method: 'put', url: `/admin/users/${id}/password`, data: { newPassword } })
}

// ==================== 分类 / 标签管理 ====================

export function apiPublicCategories() {
  return request<CategoryItem[]>({ method: 'get', url: '/categories' })
}

export function apiAdminCategoryCreate(data: { name: string; description?: string }) {
  return request<CategoryItem>({ method: 'post', url: '/admin/categories', data })
}

export function apiAdminCategoryUpdate(id: number, data: { name: string; description?: string }) {
  return request<CategoryItem>({ method: 'put', url: `/admin/categories/${id}`, data })
}

export function apiAdminCategoryDelete(id: number) {
  return request<void>({ method: 'delete', url: `/admin/categories/${id}` })
}

export function apiAdminTags() {
  return request<AdminTagItem[]>({ method: 'get', url: '/admin/tags' })
}

export function apiAdminTagRename(id: number, name: string) {
  return request<{ id: number; name: string }>({ method: 'put', url: `/admin/tags/${id}`, data: { name } })
}

export function apiAdminTagDelete(id: number) {
  return request<void>({ method: 'delete', url: `/admin/tags/${id}` })
}

// ==================== 意见反馈 ====================

export function apiAdminFeedbacks(params: PageParams) {
  return request<ApiPage<FeedbackItem>>({ method: 'get', url: '/admin/feedbacks', params })
}

export function apiAdminFeedbackStatus(id: number, status: 'pending' | 'processed') {
  return request<FeedbackItem>({ method: 'put', url: `/admin/feedbacks/${id}/status`, data: { status } })
}

export function apiAdminFeedbackDelete(id: number) {
  return request<void>({ method: 'delete', url: `/admin/feedbacks/${id}` })
}
