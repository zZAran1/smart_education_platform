/**
 * 与后端逐字段对齐的 DTO/VO 类型 + 常量
 * （下划线命名与后端一致，避免字段漂移）
 */

/* ---------- 通用 ---------- */
export interface Result<T = unknown> {
  code: number
  msg: string | null
  data: T
}

export interface Page<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages?: number
}

/* ---------- 常量 ---------- */
/** 课程类型：0理论 1实训 2认证 */
export const COURSE_TYPE_OPTIONS = [
  { value: 0, label: '理论课程', name: 'theory' },
  { value: 1, label: '实训课程', name: 'practical' },
  { value: 2, label: '认证课程', name: 'certification' },
] as const

export const COURSE_TYPE_MAP = new Map<number, string>(
  COURSE_TYPE_OPTIONS.map((o) => [o.value, o.label]),
)

/** 课程等级：0初级 1中级 2高级 3专业共建合作课程 */
export const COURSE_LEVEL_OPTIONS = [
  { value: 0, label: '初级' },
  { value: 1, label: '中级' },
  { value: 2, label: '高级' },
  { value: 3, label: '专业共建合作课程' },
] as const

export const COURSE_LEVEL_MAP = new Map<number, string>(
  COURSE_LEVEL_OPTIONS.map((o) => [o.value, o.label]),
)

/** 排序维度 */
export const COURSE_SORT_OPTIONS = [
  { value: 'publish_time', label: '最新' },
  { value: 'student_count', label: '最热' },
  { value: 'score', label: '好评' },
] as const

/** 资源类型：0课件 1视频 2实验 */
export const RESOURCE_TYPE_MAP = new Map<number, string>([
  [0, '课件'],
  [1, '视频'],
  [2, '实验'],
])

/** 成功码（与后端 Result 约定一致） */
export const SUCCESS_CODE = 200
/** Token 失效码（前端收到后清除登录态并跳转登录） */
export const TOKEN_EXPIRED_CODE = 1002

/* ---------- 用户模块 ---------- */
export interface CaptchaVO {
  uuid: string
  img_base64: string
}

export interface UserVO {
  id: number
  username: string
  nickname: string | null
  real_name: string | null
  avatar: string | null
  email: string | null
  phone: string | null
  role: number
  status: number
  created_at: string
}

export interface LoginVO {
  token: string
  user_vo: UserVO
}

export interface RegisterParams {
  username: string
  password: string
  confirm_password: string
  email?: string
  phone?: string
}

export interface LoginParams {
  username: string
  password: string
  uuid: string
  captcha: string
}

/* ---------- 课程模块 ---------- */
export interface CourseCategoryVO {
  id: number
  parent_id: number
  name: string
  type: number
  sort: number
  children?: CourseCategoryVO[]
}

export interface CourseCardVO {
  id: number
  title: string
  type: number
  tech_system_id: number | null
  tech_direction_id: number | null
  level: number
  cover: string | null
  teacher_id: number | null
  teacher_name: string | null
  courseware_count: number
  video_count: number
  lab_count: number
  is_free: number
  price: number | string
  score: number | string
  rating_count: number
  student_count: number
  publish_time: string | null
}

export interface CourseDetailVO extends CourseCardVO {
  intro: string | null
  target: string | null
  created_at: string
  is_collected: boolean
  is_enrolled: boolean
  progress: number
}

export interface CourseChapterVO {
  id: number
  course_id: number
  title: string
  resource_type: number
  duration: number
  sort: number
}

export interface CourseQueryParams {
  /** 0理论 1实训 2认证 */
  type: number
  tech_system_id?: number | null
  tech_direction_id?: number | null
  level?: number | null
  is_free?: number | null
  keyword?: string
  sort_by?: string
  page_num?: number
  page_size?: number
}

/** 格式化数字（金额/评分等以字符串返回时转 number） */
export function toNumber(v: number | string | null | undefined): number {
  if (v === null || v === undefined || v === '') return 0
  return typeof v === 'number' ? v : Number.parseFloat(v) || 0
}

/** 秒数格式化为 分:秒 / 时:分:秒 */
export function formatDuration(seconds: number): string {
  if (!seconds || seconds <= 0) return '0:00'
  const h = Math.floor(seconds / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  const pad = (n: number) => String(n).padStart(2, '0')
  return h > 0 ? `${h}:${pad(m)}:${pad(s)}` : `${m}:${pad(s)}`
}

/* ---------- 业务状态常量 ---------- */
/** 用户角色：0学员 1教师 2管理员 */
export const ROLE = { STUDENT: 0, TEACHER: 1, ADMIN: 2 } as const
export const ROLE_MAP = new Map<number, string>([
  [ROLE.STUDENT, '学员'],
  [ROLE.TEACHER, '教师'],
  [ROLE.ADMIN, '管理员'],
])

/** 用户状态：1正常 2封禁 */
export const USER_STATUS = { NORMAL: 1, BANNED: 2 } as const

/** 职位状态：0下架 1上架 */
export const JOB_STATUS = { OFF: 0, ON: 1 } as const

/** 订单状态：0待支付 1已支付 2已退款 3已取消 */
export const ORDER_STATUS = { PENDING: 0, PAID: 1, REFUNDED: 2, CANCELED: 3 } as const
export const ORDER_STATUS_MAP = new Map<number, string>([
  [ORDER_STATUS.PENDING, '待支付'],
  [ORDER_STATUS.PAID, '已支付'],
  [ORDER_STATUS.REFUNDED, '已退款'],
  [ORDER_STATUS.CANCELED, '已取消'],
])

/** 职位申请状态：0待处理 1已查看 2通过 3拒绝 */
export const APPLICATION_STATUS = { PENDING: 0, VIEWED: 1, PASSED: 2, REJECTED: 3 } as const
export const APPLICATION_STATUS_MAP = new Map<number, string>([
  [APPLICATION_STATUS.PENDING, '待处理'],
  [APPLICATION_STATUS.VIEWED, '已查看'],
  [APPLICATION_STATUS.PASSED, '通过'],
  [APPLICATION_STATUS.REJECTED, '拒绝'],
])

/** AI 面试状态：0待进行 1进行中 2已完成 */
export const INTERVIEW_STATUS = { TODO: 0, DOING: 1, DONE: 2 } as const

/** 答疑状态：0待回复 1已回复 */
export const QUESTION_STATUS = { PENDING: 0, ANSWERED: 1 } as const

/* ---------- 支付 ---------- */
/** 支付方式：0 支付宝 1 微信（仅作记录，课程设计不接入真实支付渠道） */
export const PAY_TYPE_OPTIONS = [
  { value: 0, label: '支付宝' },
  { value: 1, label: '微信支付' },
] as const

/* ---------- 实习就业模块 ---------- */
export interface JobCategoryVO {
  id: number
  parent_id: number
  name: string
  sort: number
  children?: JobCategoryVO[]
}

export interface JobCardVO {
  id: number
  title: string
  city: string | null
  address: string | null
  salary_min: number | null
  salary_max: number | null
  headcount: number
  company_id: number
  category_id: number | null
  company_name: string | null
  company_logo: string | null
  company_industry: string | null
  company_scale: string | null
  company_region: string | null
}

export interface JobDetailVO extends JobCardVO {
  description: string | null
  requirement: string | null
  company_intro: string | null
  is_collected: boolean
  is_applied: boolean
}

export interface JobQueryParams {
  category_id?: number | null
  /** job 按职位搜索 / company 按公司搜索 */
  search_type?: string
  keyword?: string
  page_num?: number
  page_size?: number
}

/** 职位薪资展示：8-12K / 15K起 / 面议 */
export function formatSalary(min?: number | null, max?: number | null): string {
  if (min == null && max == null) return '面议'
  if (min != null && max != null) return `${min}-${max}K`
  if (min != null) return `${min}K起`
  return `${max}K以内`
}

/* ---------- 课程评论 / 答疑 / 报名 ---------- */
export interface CourseCommentVO {
  id: number
  course_id: number
  user_id: number
  score: number
  content: string | null
  created_at: string
  nickname: string | null
  avatar: string | null
}

/** 课程答疑（前台展示，含提问人昵称） */
export interface CourseQuestionVO {
  id: number
  course_id: number
  user_id: number
  question: string
  answer: string | null
  answerer_id: number | null
  status: number
  created_at: string
  nickname: string | null
}

/* ---------- 后台：章节 / 公司 ---------- */
/** 课程章节（后台管理） */
export interface CourseChapter {
  id: number
  course_id: number
  title: string
  resource_type: number
  duration: number
  sort: number
}

export interface AdminChapterParams {
  title: string
  resource_type: number
  duration?: number
  sort?: number
}

/** 公司 */
export interface Company {
  id: number
  name: string
  logo: string | null
  industry: string | null
  scale: string | null
  region: string | null
  intro: string | null
}

export interface AdminCompanyParams {
  name: string
  logo?: string | null
  industry?: string | null
  scale?: string | null
  region?: string | null
  intro?: string | null
}

/** 后台评论视图（含课程标题与评论人昵称） */
export interface AdminCommentVO {
  id: number
  course_id: number
  course_title: string | null
  user_id: number
  nickname: string | null
  score: number
  content: string | null
  status: number
  created_at: string
}

/** 后台答疑视图（含课程标题与提问人昵称） */
export interface AdminQuestionVO {
  id: number
  course_id: number
  course_title: string | null
  user_id: number
  nickname: string | null
  question: string
  answer: string | null
  status: number
  created_at: string
}

export interface CourseEnrollVO {
  enrolled: boolean
  order_no: string | null
}

/* ---------- 用户资料 / 重置密码 ---------- */
export interface ResetPasswordParams {
  target: string
  code: string
  new_password: string
  confirm_password: string
}

export interface UpdateProfileParams {
  nickname?: string
  real_name?: string
}

/* ---------- 后台管理 ---------- */
export interface Course {
  id: number
  title: string
  type: number
  tech_system_id: number | null
  tech_direction_id: number | null
  level: number
  cover: string | null
  teacher_id: number | null
  teacher_name: string | null
  courseware_count: number
  video_count: number
  lab_count: number
  is_free: number
  price: number | string
  score: number | string
  rating_count: number
  student_count: number
  intro: string | null
  target: string | null
  status: number
  publish_time: string | null
}

export interface Job {
  id: number
  title: string
  city: string | null
  address: string | null
  salary_min: number | null
  salary_max: number | null
  headcount: number
  company_id: number
  category_id: number | null
  description: string | null
  requirement: string | null
  status: number
  expire_time: string | null
}

export interface OrderInfo {
  id: number
  order_no: string
  user_id: number
  course_id: number
  amount: number | string
  pay_type: number | null
  status: number
  pay_time: string | null
  created_at: string
}

export interface JobApplication {
  id: number
  user_id: number
  job_id: number
  resume_url: string | null
  status: number
  created_at: string
}

export interface AiInterview {
  id: number
  user_id: number
  job_id: number
  application_id: number
  status: number
  report: string | null
  interview_time: string | null
  created_at: string
}

export interface AdminCourseParams {
  title: string
  type: number
  tech_system_id?: number | null
  tech_direction_id?: number | null
  level?: number | null
  cover?: string | null
  teacher_id?: number | null
  teacher_name?: string | null
  courseware_count?: number
  video_count?: number
  lab_count?: number
  is_free?: number
  price?: number
  intro?: string | null
  target?: string | null
}

export interface AdminJobParams {
  title: string
  city?: string | null
  address?: string | null
  salary_min?: number | null
  salary_max?: number | null
  headcount?: number
  company_id: number
  category_id?: number | null
  expire_time?: string | null
  description?: string | null
  requirement?: string | null
}
