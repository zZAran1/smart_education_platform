import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  server: {
    port: 5173,
    proxy: {
      // 前端 /api 请求转发到后端服务
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      // 头像等上传文件由后端 /uploads/** 提供，必须一并转发：
      // 否则会命中 Vite 的 SPA 回退返回 index.html，<img> 拿到 HTML 导致图片不显示
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
