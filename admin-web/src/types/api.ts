/** 后端接口类型定义（与 docs/API_CONTRACT.md 对应，null 字段后端不输出） */

export interface ApiPage<T> {
  items: T[]
  total: number
  page: number
  pageSize: number
  pages: number
}

export interface BlogAuthor {
  id: number
  username: string
  nickname?: string
  avatar?: string
}

export interface TagItem {
  id: number
  name: string
}

export interface BlogSummary {
  id: number
  author?: BlogAuthor
  title: string
  summary?: string
  coverImage?: string
  categoryId?: number
  categoryName?: string
  tags?: TagItem[]
  status: 'draft' | 'published' | 'offline'
  isPublic?: boolean
  viewCount?: number
  publishedAt?: string
  createdAt?: string
  updatedAt?: string
}

export interface BlogDetail extends BlogSummary {
  content?: object
  contentHtml?: string
  contentVersion?: string
}

export interface CategoryItem {
  id: number
  name: string
  description?: string
}

export interface AdminTagItem {
  id: number
  name: string
  blogCount: number
}

export interface AdminUserItem {
  id: number
  username: string
  nickname?: string
  email?: string
  avatar?: string
  role: 'user' | 'admin'
  status: 0 | 1
  blogCount?: number
  createdAt?: string
}

export interface FeedbackItem {
  id: number
  userId?: number
  username?: string
  nickname?: string
  avatar?: string
  contact?: string
  content: string
  status: 'pending' | 'processed'
  createdAt?: string
}

export interface TrendPoint {
  date: string
  blogsPublished: number
  newUsers: number
}

export interface AdminStats {
  totalUsers: number
  totalBlogs: number
  publishedBlogs: number
  draftBlogs: number
  offlineBlogs: number
  totalViews: number
  totalCategories: number
  totalTags: number
  totalFeedbacks: number
  pendingFeedbacks: number
  trend: TrendPoint[]
}

export interface CurrentUser {
  id: number
  username: string
  nickname?: string
  email?: string
  avatar?: string
  bio?: string
  role?: 'user' | 'admin'
  createdAt?: string
}

export interface LoginResult {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: CurrentUser
}

/** 管理端：用户会话（只读查看聊天记录） */
export interface AdminConversationItem {
  id: number
  other?: BlogAuthor
  lastMessage?: { senderId?: number; content?: string; at?: string }
  unreadCount?: number
  updatedAt?: string
}

/** 管理端：会话消息（只读查看聊天记录） */
export interface AdminMessageItem {
  id: number
  conversationId: number
  sender?: BlogAuthor
  content?: string
  msgType?: 'text' | 'image'
  imageUrl?: string
  imageWidth?: number
  imageHeight?: number
  isRead?: boolean
  createdAt?: string
}
