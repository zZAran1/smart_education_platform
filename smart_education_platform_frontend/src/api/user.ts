import { http } from './request'
import type {
  CaptchaVO,
  LoginParams,
  LoginVO,
  RegisterParams,
  ResetCodeVO,
  ResetPasswordParams,
  UpdateProfileParams,
  UserVO,
} from '@/types/api'

/** 获取登录图形验证码 */
export const getCaptcha = () => http.get<CaptchaVO>('/user/captcha')

/** 用户注册 */
export const registerUser = (data: RegisterParams) => http.post<void>('/user/register', data)

/** 用户登录（验证码一次性） */
export const loginUser = (data: LoginParams) => http.post<LoginVO>('/user/login', data)

/** 下发重置密码验证码（开发环境 code 直接回传） */
export const sendResetCode = (target: string) => http.post<ResetCodeVO>('/user/reset-code', { target })

/** 通过验证码重置密码 */
export const resetPassword = (data: ResetPasswordParams) => http.post<void>('/user/reset-password', data)

/** 当前登录用户信息 */
export const getProfile = () => http.get<UserVO>('/user/profile')

/** 修改昵称/姓名 */
export const updateProfile = (data: UpdateProfileParams) => http.put<UserVO>('/user/profile', data)

/** 上传头像（JPG/PNG ≤2MB） */
export const updateAvatar = (file: File) => http.upload<string>('/user/avatar', file)

/** 退出登录（Token 加入黑名单） */
export const logout = () => http.post<void>('/user/logout')
