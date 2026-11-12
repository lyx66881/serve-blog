import { defineStore } from 'pinia'
import { apiLogin, apiLogout } from '../api/admin'
import { clearAuth, getToken, TOKEN_KEY, USER_KEY } from '../api/http'
import type { CurrentUser } from '../types/api'

function loadUser(): CurrentUser | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as CurrentUser
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: getToken(),
    user: loadUser() as CurrentUser | null
  }),
  getters: {
    isLoggedIn: state => !!state.token,
    isAdmin: state => state.user?.role === 'admin'
  },
  actions: {
    async login(username: string, password: string) {
      const result = await apiLogin(username, password)
      if (result.user.role !== 'admin') {
        throw new Error('当前账号不是管理员，无法进入后台')
      }
      this.token = result.accessToken
      this.user = result.user
      localStorage.setItem(TOKEN_KEY, result.accessToken)
      localStorage.setItem(USER_KEY, JSON.stringify(result.user))
    },
    async logout() {
      try {
        await apiLogout()
      } catch {
        // token 已失效也继续清理本地状态
      }
      this.token = null
      this.user = null
      clearAuth()
    }
  }
})
