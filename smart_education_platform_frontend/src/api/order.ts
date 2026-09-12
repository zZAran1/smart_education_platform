import { http } from './request'
import type { MyOrderVO, Page } from '@/types/api'

/**
 * 支付订单。
 * 课程设计简化实现：不接入真实支付渠道，点击支付即视为支付成功并开通课程。
 * pay_type 仅作记录（0 支付宝 1 微信）。
 */
export const payOrder = (orderNo: string, pay_type?: number) =>
  http.post<void>(`/order/${orderNo}/pay`, { pay_type })

/** 我的订单：当前登录用户的订单记录 + 课程标题（后端按 Token 识别用户） */
export const getMyOrders = (page_num = 1, page_size = 10) =>
  http.get<Page<MyOrderVO>>('/order/my', { page_num, page_size })
