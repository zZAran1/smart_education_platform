import { http } from './request'
import type {
  CourseCategoryVO,
  CourseChapterVO,
  CourseCommentVO,
  CourseDetailVO,
  CourseEnrollVO,
  CourseQueryParams,
  CourseQuestionVO,
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

/** 收藏/取消收藏（切换），返回收藏后状态 */
export const toggleCourseCollect = (id: number) => http.post<boolean>(`/course/${id}/collect`)

/** 立即学习：免费直接报名，收费生成待支付订单 */
export const enrollCourse = (id: number) => http.post<CourseEnrollVO>(`/course/${id}/enroll`)

/** 课程评论分页 */
export const getCourseComments = (id: number, page_num = 1, page_size = 10) =>
  http.get<Page<CourseCommentVO>>(`/course/${id}/comments`, { page_num, page_size })

/** 发表课程评论（须已报名） */
export const addCourseComment = (id: number, data: { score: number; content?: string }) =>
  http.post<void>(`/course/${id}/comments`, data)

/** 课程答疑列表 */
export const getCourseQuestions = (id: number) => http.get<CourseQuestionVO[]>(`/course/${id}/questions`)

/** 提交答疑问题 */
export const addCourseQuestion = (id: number, question: string) =>
  http.post<void>(`/course/${id}/questions`, { question })

/** 标记章节学完，更新学习进展 */
export const finishChapter = (chapterId: number) => http.post<void>(`/study/chapters/${chapterId}/finish`)

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
