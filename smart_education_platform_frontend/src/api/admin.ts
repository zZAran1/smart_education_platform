import { http } from './request'
import type {
  AdminCourseParams,
  AdminJobParams,
  AiInterview,
  Course,
  Job,
  JobApplication,
  OrderInfo,
  Page,
  UserVO,
} from '@/types/api'

/* ---------- 用户管理 ---------- */
export const getAdminUsers = (params: Record<string, unknown>) => http.get<Page<UserVO>>('/admin/users', params)
export const updateUserStatus = (id: number, status: number) => http.put<void>(`/admin/users/${id}/status?status=${status}`)

/* ---------- 订单管理 ---------- */
export const getAdminOrders = (params: Record<string, unknown>) => http.get<Page<OrderInfo>>('/admin/orders', params)
export const refundOrder = (id: number) => http.put<void>(`/admin/orders/${id}/refund`)

/* ---------- 课程管理 ---------- */
export const getAdminCourses = (params: Record<string, unknown>) => http.get<Page<Course>>('/admin/courses', params)
export const createCourse = (data: AdminCourseParams) => http.post<number>('/admin/courses', data)
export const updateCourse = (id: number, data: AdminCourseParams) => http.put<void>(`/admin/courses/${id}`, data)
export const updateCourseStatus = (id: number, status: number) => http.put<void>(`/admin/courses/${id}/status?status=${status}`)

/* ---------- 职位管理 ---------- */
export const getAdminJobs = (params: Record<string, unknown>) => http.get<Page<Job>>('/admin/jobs', params)
export const createJob = (data: AdminJobParams) => http.post<number>('/admin/jobs', data)
export const updateJob = (id: number, data: AdminJobParams) => http.put<void>(`/admin/jobs/${id}`, data)
export const updateJobStatus = (id: number, status: number) => http.put<void>(`/admin/jobs/${id}/status?status=${status}`)

/* ---------- 申请管理 ---------- */
export const getAdminApplications = (params: Record<string, unknown>) => http.get<Page<JobApplication>>('/admin/applications', params)
export const updateApplicationStatus = (id: number, status: number) => http.put<void>(`/admin/applications/${id}/status?status=${status}`)

/* ---------- 面试管理 ---------- */
export const getAdminInterviews = (params: Record<string, unknown>) => http.get<Page<AiInterview>>('/admin/interviews', params)
export const updateInterview = (id: number, status: number) => http.put<void>(`/admin/interviews/${id}?status=${status}`)
