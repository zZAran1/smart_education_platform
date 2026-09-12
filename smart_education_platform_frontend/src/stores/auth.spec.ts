import { beforeEach, describe, expect, it } from 'vitest'

import type { UserVO } from '@/types/api'

import { authState, clearAuth, getToken, isLoggedIn, setAuth, setUserInfo } from './auth'

const TOKEN_KEY = 'edu_platform_token'
const USER_KEY = 'edu_platform_user'

const mockUser = {
  id: 3,
  username: 'student01',
  nickname: '小明同学',
  role: 0,
} as UserVO

describe('登录态 store', () => {
  beforeEach(() => {
    clearAuth()
    localStorage.clear()
  })

  it('初始状态未登录', () => {
    expect(getToken()).toBe('')
    expect(isLoggedIn()).toBe(false)
    expect(authState.userInfo).toBeNull()
  })

  it('setAuth 同时写入内存与 localStorage', () => {
    setAuth('token-abc', mockUser)

    expect(getToken()).toBe('token-abc')
    expect(isLoggedIn()).toBe(true)
    expect(authState.userInfo?.username).toBe('student01')
    expect(localStorage.getItem(TOKEN_KEY)).toBe('token-abc')
    expect(JSON.parse(localStorage.getItem(USER_KEY) as string).username).toBe('student01')
  })

  it('clearAuth 清空内存与 localStorage（退出登录）', () => {
    setAuth('token-abc', mockUser)
    clearAuth()

    expect(getToken()).toBe('')
    expect(isLoggedIn()).toBe(false)
    expect(authState.userInfo).toBeNull()
    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem(USER_KEY)).toBeNull()
  })

  it('setUserInfo 只更新用户信息，不影响 token', () => {
    setAuth('token-abc', mockUser)
    setUserInfo({ ...mockUser, nickname: '改名后' } as UserVO)

    expect(getToken()).toBe('token-abc')
    expect(authState.userInfo?.nickname).toBe('改名后')
    expect(JSON.parse(localStorage.getItem(USER_KEY) as string).nickname).toBe('改名后')
  })

  it('登录态可通过 localStorage 持久化（模拟刷新页面后的读取）', () => {
    setAuth('token-persist', mockUser)

    // 模拟新会话：直接读 localStorage，应与写入值一致
    expect(localStorage.getItem(TOKEN_KEY)).toBe('token-persist')
    const restored = JSON.parse(localStorage.getItem(USER_KEY) as string) as UserVO
    expect(restored.id).toBe(3)
    expect(restored.role).toBe(0)
  })
})
