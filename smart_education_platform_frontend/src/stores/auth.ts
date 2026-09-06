/**
 * 登录态：reactive 模块单例 + localStorage 持久化
 * key = edu_platform_token
 */
import { reactive } from 'vue'
import type { UserVO } from '@/types/api'

const TOKEN_KEY = 'edu_platform_token'
const USER_KEY = 'edu_platform_user'

interface AuthState {
  token: string
  userInfo: UserVO | null
}

function readUser(): UserVO | null {
  try {
    return JSON.parse(localStorage.getItem(USER_KEY) || 'null') as UserVO | null
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}

const state = reactive<AuthState>({
  token: localStorage.getItem(TOKEN_KEY) || '',
  userInfo: readUser(),
})

export function getToken(): string {
  return state.token
}

export function isLoggedIn(): boolean {
  return !!state.token
}

export function setAuth(token: string, userInfo: UserVO): void {
  state.token = token
  state.userInfo = userInfo
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
}

export function setUserInfo(userInfo: UserVO): void {
  state.userInfo = userInfo
  localStorage.setItem(USER_KEY, JSON.stringify(userInfo))
}

export function clearAuth(): void {
  state.token = ''
  state.userInfo = null
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

/** 供模板直接消费的响应式对象 */
export const authState = state
