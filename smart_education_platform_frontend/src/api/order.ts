import { http } from './request'

/**
 * 支付订单。
 * 课程设计简化实现：不接入真实支付渠道，点击支付即视为支付成功并开通课程。
 * pay_type 仅作记录（0 支付宝 1 微信）。
 */
export const payOrder = (orderNo: string, pay_type?: number) =>
  http.post<void>(`/order/${orderNo}/pay`, { pay_type })
