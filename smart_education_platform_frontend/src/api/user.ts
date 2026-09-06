import { http } from './request'
import type { CaptchaVO, LoginParams, LoginVO, RegisterParams } from '@/types/api'

/** 获取登录图形验证码 */
export const getCaptcha = () => http.get<CaptchaVO>('/user/captcha')

/** 用户注册 */
export const registerUser = (data: RegisterParams) => http.post<void>('/user/register', data)

/** 用户登录（验证码一次性） */
export const loginUser = (data: LoginParams) => http.post<LoginVO>('/user/login', data)
