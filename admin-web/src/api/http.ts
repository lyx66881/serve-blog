import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 统一响应结构 */
export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export const TOKEN_KEY = 'admin_token'
export const USER_KEY = 'admin_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function clearAuth() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

const http = axios.create({
  baseURL: '/api/v1',
  timeout: 15000
})

http.interceptors.request.use(config => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  response => {
    const body = response.data as ApiResponse<unknown>
    // 上传/静态等非常规场景兜底
    if (typeof body?.code !== 'number') {
      return response
    }
    if (body.code === 0) {
      return response
    }
    if ([40100, 40102, 40103].includes(body.code)) {
      clearAuth()
      ElMessage.error(body.message || '登录已过期，请重新登录')
      if (!location.pathname.startsWith('/login')) {
        setTimeout(() => { location.href = '/login' }, 500)
      }
    }
    return Promise.reject(new Error(body.message || `请求失败（${body.code}）`))
  },
  error => {
    // HTTP 层错误：401（未认证）、403（无权限）等同样带统一 JSON
    const body = error?.response?.data as ApiResponse<unknown> | undefined
    const message = body?.message || error.message || '网络异常'
    const code = body?.code
    if (code !== undefined && [40100, 40102, 40103].includes(code)) {
      clearAuth()
      if (!location.pathname.startsWith('/login')) {
        setTimeout(() => { location.href = '/login' }, 500)
      }
    }
    return Promise.reject(new Error(message))
  }
)

/** 解包 ApiResponse<T>，直接返回 data；业务失败抛 Error(message) */
export async function request<T>(config: Parameters<typeof http.request>[0]): Promise<T> {
  const response = await http.request<ApiResponse<T>>(config)
  return response.data.data
}

export default http
