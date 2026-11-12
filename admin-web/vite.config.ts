import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 开发服务器代理：/api 与 /uploads 转发到 Spring Boot 后端（默认 8080）
export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: process.env.BACKEND_ORIGIN || 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/uploads': {
        target: process.env.BACKEND_ORIGIN || 'http://127.0.0.1:8080',
        changeOrigin: true
      }
    }
  }
})
