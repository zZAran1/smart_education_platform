/**
 * 轻量 toast：模块级 reactive 消息队列，由 <AppToast/> 渲染
 */
import { reactive } from 'vue'

export interface ToastItem {
  id: number
  type: 'success' | 'error' | 'info'
  message: string
}

const state = reactive<{ list: ToastItem[] }>({ list: [] })

let seed = 0

export function showToast(message: string, type: ToastItem['type'] = 'info', duration = 2600): void {
  const id = ++seed
  state.list.push({ id, type, message })
  setTimeout(() => removeToast(id), duration)
}

export function removeToast(id: number): void {
  const index = state.list.findIndex((item) => item.id === id)
  if (index !== -1) {
    state.list.splice(index, 1)
  }
}

export const toastState = state
