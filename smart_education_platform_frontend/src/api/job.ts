import { http } from './request'
import type {
  JobCardVO,
  JobCategoryVO,
  JobDetailVO,
  JobQueryParams,
  MyInterviewVO,
  MyJobApplicationVO,
  Page,
} from '@/types/api'

/** 职位分类树（两级） */
export const getJobCategories = () => http.get<JobCategoryVO[]>('/job/categories')

/** 职位分页（分类 + 职位/公司搜索） */
export const getJobPage = (params: JobQueryParams) =>
  http.get<Page<JobCardVO>>('/job/page', buildQuery(params))

/** 职位详情（登录后返回收藏/投递状态） */
export const getJobDetail = (id: number) => http.get<JobDetailVO>(`/job/${id}`)

/** 感兴趣（收藏切换），返回收藏后状态 */
export const toggleJobCollect = (id: number) => http.post<boolean>(`/job/${id}/collect`)

/** 申请职位 */
export const applyJob = (id: number, resume_url?: string) =>
  http.post<void>(`/job/${id}/apply`, { resume_url })

/** 申请 AI 面试（须已投递），返回面试记录 ID */
export const applyAiInterview = (id: number) => http.post<number>(`/job/${id}/ai-interview`)

/** 我的投递：当前登录用户的投递记录 + 职位/公司信息（后端按 Token 识别用户） */
export const getMyApplications = (page_num = 1, page_size = 10) =>
  http.get<Page<MyJobApplicationVO>>('/job/my-applications', { page_num, page_size })

/** 我的数字人面试：当前登录用户的面试记录 + 职位/公司信息 */
export const getMyInterviews = (page_num = 1, page_size = 10) =>
  http.get<Page<MyInterviewVO>>('/job/my-interviews', { page_num, page_size })

function buildQuery(params: JobQueryParams): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query[key] = value
    }
  })
  return query
}
