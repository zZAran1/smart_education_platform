import { http } from './request'
import type {
  CourseCategoryVO,
  CourseChapterVO,
  CourseDetailVO,
  CourseQueryParams,
  Page,
  CourseCardVO,
} from '@/types/api'

/** 课程分类树（两级：技术体系 / 技术方向） */
export const getCourseCategories = (type?: number) =>
  http.get<CourseCategoryVO[]>('/course/categories', type === undefined ? {} : { type })

/** 课程中心分页（筛选 + 关键词搜索 + 排序） */
export const getCoursePage = (params: CourseQueryParams) =>
  http.get<Page<CourseCardVO>>('/course/page', buildQuery(params))

/** 课程详情（登录后返回收藏/报名/进展增强字段） */
export const getCourseDetail = (id: number) => http.get<CourseDetailVO>(`/course/${id}`)

/** 课程目录（章节列表） */
export const getCourseChapters = (id: number) => http.get<CourseChapterVO[]>(`/course/${id}/chapters`)

/** 过滤掉空值参数，仅保留有效查询条件 */
function buildQuery(params: CourseQueryParams): Record<string, unknown> {
  const query: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      query[key] = value
    }
  })
  return query
}
