/**
 * axios 封装：baseURL=/api，请求自动携带 Bearer token，
 * 响应统一拆包 Result，code===1002 时清除登录态并跳转登录页。
 */
import axios, { type AxiosRequestConfig } from 'axios'
import { getToken, clearAuth } from '@/stores/auth'
import { SUCCESS_CODE, TOKEN_EXPIRED_CODE, type Result } from '@/types/api'
import { showToast } from '@/composables/toast'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const res = response.data as Result
    if (res.code === SUCCESS_CODE) {
      return res.data as never
    }
    // Token 失效：清除登录态并回到登录页
    if (res.code === TOKEN_EXPIRED_CODE) {
      clearAuth()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    // 业务错误统一提示
    showToast(res.msg || '请求失败', 'error')
    return Promise.reject(new Error(res.msg || '请求失败'))
  },
  (error) => {
    const msg =
      error.response?.data?.msg ||
      (error.code === 'ECONNABORTED' ? '请求超时，请稍后重试' : '网络异常，请稍后重试')
    showToast(msg, 'error')
    return Promise.reject(new Error(msg))
  },
)

/** 泛型请求：响应拦截器已拆包 Result，直接断言返回 Result.data */
async function request<T>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>
}

export const http = {
  get: <T>(url: string, params?: Record<string, unknown>) => request<T>({ url, method: 'get', params }),
  post: <T>(url: string, data?: unknown) => request<T>({ url, method: 'post', data }),
}

export default http
