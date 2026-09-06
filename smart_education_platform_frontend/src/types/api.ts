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
