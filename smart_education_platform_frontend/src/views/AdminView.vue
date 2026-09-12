<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  getAdminUsers,
  updateUserStatus,
  getAdminOrders,
  refundOrder,
  getAdminCourses,
  createCourse,
  updateCourse,
  updateCourseStatus,
  getAdminJobs,
  createJob,
  updateJob,
  updateJobStatus,
  getAdminApplications,
  updateApplicationStatus,
  getAdminInterviews,
  updateInterview,
  getAdminComments,
  hideComment,
  getAdminQuestions,
  answerQuestion,
  getAdminChapters,
  createChapter,
  updateChapter,
  deleteChapter,
  getAdminCompanies,
  createCompany,
  updateCompany,
  deleteCompany,
} from '@/api/admin'
import { showToast } from '@/composables/toast'
import { getJobCategories } from '@/api/job'
import {
  APPLICATION_STATUS_MAP,
  COURSE_TYPE_MAP,
  ORDER_STATUS_MAP,
  RESOURCE_TYPE_MAP,
  ROLE_MAP,
  USER_STATUS,
  formatDuration,
  formatSalary,
  toNumber,
  type AdminChapterParams,
  type AdminCommentVO,
  type AdminCompanyParams,
  type AdminCourseParams,
  type AdminJobParams,
  type AdminQuestionVO,
  type AiInterview,
  type Company,
  type Course,
  type CourseChapter,
  type Job,
  type JobApplication,
  type JobCategoryVO,
  type OrderInfo,
  type UserVO,
} from '@/types/api'

type Tab =
  | 'users'
  | 'courses'
  | 'jobs'
  | 'orders'
  | 'applications'
  | 'interviews'
  | 'companies'
  | 'comments'
  | 'questions'

const PAGE_SIZE = 10

const activeTab = ref<Tab>('users')
const loading = ref(false)
/** 表单提交中标记：防止重复点击写入重复数据 */
const saving = ref(false)
/** 请求序号：丢弃过期响应，避免快速切换 tab 时旧数据覆盖新数据 */
let loadSeq = 0

const users = ref<UserVO[]>([])
const courses = ref<Course[]>([])
const jobs = ref<Job[]>([])
const orders = ref<OrderInfo[]>([])
const applications = ref<JobApplication[]>([])
const interviews = ref<AiInterview[]>([])
const companies = ref<Company[]>([])
const adminComments = ref<AdminCommentVO[]>([])
const adminQuestions = ref<AdminQuestionVO[]>([])

const total = ref(0)
const pageNum = ref(1)

/** 当前 tab 的列表条数（用于删除后判断是否需要回收页码） */
const currentListLength = computed(() => {
  switch (activeTab.value) {
    case 'users': return users.value.length
    case 'courses': return courses.value.length
    case 'jobs': return jobs.value.length
    case 'orders': return orders.value.length
    case 'applications': return applications.value.length
    case 'interviews': return interviews.value.length
    case 'companies': return companies.value.length
    case 'comments': return adminComments.value.length
    default: return adminQuestions.value.length
  }
})

/** 公司表单 */
const showCompanyForm = ref(false)
const editingCompanyId = ref<number | null>(null)
const companyForm = reactive<AdminCompanyParams>({
  name: '',
  logo: '',
  industry: '',
  scale: '',
  region: '',
  intro: '',
})

/** 课程章节目录面板 */
const chapterPanel = reactive({ visible: false, courseId: 0, courseTitle: '' })
const chapters = ref<CourseChapter[]>([])
const chapterForm = reactive<AdminChapterParams>({ title: '', resource_type: 0, duration: 0, sort: 0 })
const editingChapterId = ref<number | null>(null)

/** 答疑回复 */
const answeringId = ref<number | null>(null)
const answerText = ref('')

/** 职位表单的公司下拉选项 */
const companyOptions = ref<Company[]>([])

/** 职位表单的分类下拉：一级只用于筛选二级，最终提交二级分类 id */
const jobCategories = ref<JobCategoryVO[]>([])
const rootCategoryId = ref<number | null>(null)
const childCategories = computed(() => {
  if (rootCategoryId.value == null) return []
  return jobCategories.value.find((c) => c.id === rootCategoryId.value)?.children || []
})

/* ---------- 课程表单 ---------- */
const showCourseForm = ref(false)
const editingCourseId = ref<number | null>(null)
const courseForm = reactive<AdminCourseParams>({
  title: '',
  type: 0,
  level: 0,
  is_free: 1,
  price: 0,
  teacher_name: '',
  intro: '',
  target: '',
})

/* ---------- 职位表单 ---------- */
const showJobForm = ref(false)
const editingJobId = ref<number | null>(null)
const jobForm = reactive<AdminJobParams>({
  title: '',
  city: '',
  address: '',
  salary_min: null,
  salary_max: null,
  headcount: 1,
  company_id: 0,
  category_id: null,
  expire_time: '',
  description: '',
  requirement: '',
})

const tabs: { key: Tab; label: string }[] = [
  { key: 'users', label: '用户管理' },
  { key: 'courses', label: '课程管理' },
  { key: 'jobs', label: '职位管理' },
  { key: 'orders', label: '订单管理' },
  { key: 'applications', label: '申请管理' },
  { key: 'interviews', label: '面试管理' },
  { key: 'companies', label: '公司管理' },
  { key: 'comments', label: '评论管理' },
  { key: 'questions', label: '答疑管理' },
]

async function load(): Promise<void> {
  const seq = ++loadSeq
  loading.value = true
  try {
    const params = { page_num: pageNum.value, page_size: PAGE_SIZE }
    switch (activeTab.value) {
      case 'users': {
        const data = await getAdminUsers(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { users.value = list })
        break
      }
      case 'courses': {
        const data = await getAdminCourses(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { courses.value = list })
        break
      }
      case 'jobs': {
        const data = await getAdminJobs(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { jobs.value = list })
        break
      }
      case 'orders': {
        const data = await getAdminOrders(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { orders.value = list })
        break
      }
      case 'applications': {
        const data = await getAdminApplications(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { applications.value = list })
        break
      }
      case 'interviews': {
        const data = await getAdminInterviews(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { interviews.value = list })
        break
      }
      case 'companies': {
        const data = await getAdminCompanies(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { companies.value = list })
        break
      }
      case 'comments': {
        const data = await getAdminComments(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { adminComments.value = list })
        break
      }
      case 'questions': {
        const data = await getAdminQuestions(params)
        applyResult(seq, data.records || [], data.total || 0, (list) => { adminQuestions.value = list })
        break
      }
    }
  } catch {
    // 请求失败时清空当前 tab 的列表与分页总数，避免残留上一个 tab 的 total，
    // 否则会渲染出"有表头但无数据行"的空表，而不是"暂无数据"空状态
    if (seq === loadSeq) {
      total.value = 0
      clearCurrentList()
    }
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

/** 清空当前 tab 的列表数据（请求失败时使用） */
function clearCurrentList(): void {
  switch (activeTab.value) {
    case 'users': users.value = []; break
    case 'courses': courses.value = []; break
    case 'jobs': jobs.value = []; break
    case 'orders': orders.value = []; break
    case 'applications': applications.value = []; break
    case 'interviews': interviews.value = []; break
    case 'companies': companies.value = []; break
    case 'comments': adminComments.value = []; break
    default: adminQuestions.value = []; break
  }
}

/**
 * 统一的响应落地处理：
 * ① 丢弃过期响应（已切换 tab 或发起了更新请求），避免旧数据覆盖新数据；
 * ② 当前页被删空时自动回退一页，避免出现只有表头的空表格。
 */
function applyResult<T>(seq: number, records: T[], count: number, assign: (list: T[]) => void): void {
  if (seq !== loadSeq) return
  if (records.length === 0 && pageNum.value > 1) {
    pageNum.value -= 1
    load()
    return
  }
  total.value = count
  assign(records)
}

/** 删除/隐藏操作后的刷新：若当前页只剩这一条则回收页码 */
function refreshAfterRemove(): void {
  if (currentListLength.value <= 1 && pageNum.value > 1) {
    pageNum.value -= 1
  }
  load()
}

function switchTab(tab: Tab): void {
  activeTab.value = tab
  pageNum.value = 1
  // 清理上一个 tab 的临时状态，避免跨 tab 残留（表单、章节面板、答疑展开态）
  showCourseForm.value = false
  showJobForm.value = false
  showCompanyForm.value = false
  closeChapters()
  cancelAnswer()
  load()
}

function changePage(p: number): void {
  pageNum.value = p
  load()
}

/* ---------- 用户 ---------- */
async function onToggleUser(user: UserVO): Promise<void> {
  const next = user.status === USER_STATUS.BANNED ? USER_STATUS.NORMAL : USER_STATUS.BANNED
  try {
    await updateUserStatus(user.id, next)
    showToast(next === USER_STATUS.BANNED ? '已封禁' : '已启用', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 订单 ---------- */
async function onRefund(order: OrderInfo): Promise<void> {
  try {
    await refundOrder(order.id)
    showToast('退款成功', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 课程 ---------- */
function openCreateCourse(): void {
  editingCourseId.value = null
  Object.assign(courseForm, { title: '', type: 0, level: 0, is_free: 1, price: 0, teacher_name: '', intro: '', target: '' })
  showCourseForm.value = true
}

function openEditCourse(course: Course): void {
  editingCourseId.value = course.id
  Object.assign(courseForm, {
    title: course.title,
    type: course.type,
    level: course.level,
    is_free: course.is_free,
    price: toNumber(course.price),
    teacher_name: course.teacher_name || '',
    intro: course.intro || '',
    target: course.target || '',
  })
  showCourseForm.value = true
}

async function submitCourse(): Promise<void> {
  if (!courseForm.title.trim()) {
    showToast('请输入课程名称', 'error')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    if (editingCourseId.value) {
      await updateCourse(editingCourseId.value, { ...courseForm, title: courseForm.title.trim() })
      showToast('保存成功', 'success')
    } else {
      await createCourse({ ...courseForm, title: courseForm.title.trim() })
      showToast('新增成功', 'success')
    }
    showCourseForm.value = false
    load()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function onToggleCourse(course: Course): Promise<void> {
  const next = course.status === 1 ? 0 : 1
  try {
    await updateCourseStatus(course.id, next)
    showToast(next === 1 ? '已上架' : '已下架', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 职位 ---------- */
async function openCreateJob(): Promise<void> {
  editingJobId.value = null
  Object.assign(jobForm, { title: '', city: '', address: '', salary_min: null, salary_max: null, headcount: 1, company_id: 0, category_id: null, expire_time: '', description: '', requirement: '' })
  rootCategoryId.value = null
  showJobForm.value = true
  await loadJobCategories()
  loadCompanyOptions()
}

async function openEditJob(job: Job): Promise<void> {
  editingJobId.value = job.id
  Object.assign(jobForm, {
    title: job.title,
    city: job.city || '',
    address: job.address || '',
    salary_min: job.salary_min,
    salary_max: job.salary_max,
    headcount: job.headcount,
    company_id: job.company_id,
    category_id: job.category_id,
    expire_time: job.expire_time ? job.expire_time.slice(0, 16) : '',
    description: job.description || '',
    requirement: job.requirement || '',
  })
  showJobForm.value = true
  await loadJobCategories()
  // 由已有的二级分类反查所属一级分类，保证编辑时两级下拉都能正确回填
  rootCategoryId.value =
    jobCategories.value.find((c) => (c.children || []).some((child) => child.id === job.category_id))?.id ?? null
  loadCompanyOptions()
}

/** 加载职位分类树（两级），供职位表单选择；分类基本不变，只在首次打开时拉取 */
async function loadJobCategories(): Promise<void> {
  if (jobCategories.value.length > 0) return
  try {
    jobCategories.value = await getJobCategories()
  } catch {
    /* 已提示 */
  }
}

/** 切换一级分类时清空二级选择，避免提交到与一级不匹配的二级分类 */
function onRootCategoryChange(): void {
  jobForm.category_id = null
}

/** 加载公司下拉选项（供职位表单选择所属公司） */
async function loadCompanyOptions(): Promise<void> {
  try {
    const data = await getAdminCompanies({ page_num: 1, page_size: 200 })
    companyOptions.value = data.records || []
  } catch {
    /* 已提示 */
  }
}

async function submitJob(): Promise<void> {
  if (!jobForm.title.trim() || !jobForm.company_id) {
    showToast('请填写职位名称并选择所属公司', 'error')
    return
  }
  // 分类决定该职位在前台「实习就业」按分类筛选时能否被检索到，故必填
  if (!jobForm.category_id) {
    showToast('请选择职位分类', 'error')
    return
  }
  // datetime-local 的值形如 2026-01-01T12:00，补秒后即为后端 LocalDateTime 要求的 ISO-8601
  const payload: AdminJobParams = {
    ...jobForm,
    title: jobForm.title.trim(),
    expire_time: jobForm.expire_time ? `${jobForm.expire_time}:00` : null,
  }
  if (saving.value) return
  saving.value = true
  try {
    if (editingJobId.value) {
      await updateJob(editingJobId.value, payload)
      showToast('保存成功', 'success')
    } else {
      await createJob(payload)
      showToast('新增成功', 'success')
    }
    showJobForm.value = false
    load()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function onToggleJob(job: Job): Promise<void> {
  const next = job.status === 1 ? 0 : 1
  try {
    await updateJobStatus(job.id, next)
    showToast(next === 1 ? '已上架' : '已下架', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 申请 ---------- */
async function onApplicationStatus(app: JobApplication, status: number): Promise<void> {
  try {
    await updateApplicationStatus(app.id, status)
    showToast('状态已更新', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 面试 ---------- */
async function onInterviewStatus(interview: AiInterview, status: number): Promise<void> {
  try {
    await updateInterview(interview.id, status)
    showToast('状态已更新', 'success')
    load()
  } catch {
    /* 已提示 */
  }
}

function fmtDate(v?: string | null): string {
  return v ? v.slice(0, 10) : '—'
}

/* ---------- 章节目录 ---------- */
async function openChapters(course: Course): Promise<void> {
  chapterPanel.courseId = course.id
  chapterPanel.courseTitle = course.title
  chapterPanel.visible = true
  resetChapterForm()
  await loadChapters()
}

function closeChapters(): void {
  chapterPanel.visible = false
  chapters.value = []
  resetChapterForm()
}

async function loadChapters(): Promise<void> {
  try {
    chapters.value = await getAdminChapters(chapterPanel.courseId)
  } catch {
    /* 已提示 */
  }
}

function resetChapterForm(): void {
  editingChapterId.value = null
  Object.assign(chapterForm, { title: '', resource_type: 0, duration: 0, sort: 0 })
}

function editChapter(ch: CourseChapter): void {
  editingChapterId.value = ch.id
  Object.assign(chapterForm, {
    title: ch.title,
    resource_type: ch.resource_type,
    duration: ch.duration,
    sort: ch.sort,
  })
}

async function submitChapter(): Promise<void> {
  if (!chapterForm.title.trim()) {
    showToast('请填写资源标题', 'error')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const payload = { ...chapterForm, title: chapterForm.title.trim() }
    if (editingChapterId.value) {
      await updateChapter(editingChapterId.value, payload)
    } else {
      await createChapter(chapterPanel.courseId, payload)
    }
    showToast('保存成功', 'success')
    resetChapterForm()
    await loadChapters()
    load()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function removeChapter(ch: CourseChapter): Promise<void> {
  try {
    await deleteChapter(ch.id)
    showToast('删除成功', 'success')
    await loadChapters()
    load()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 公司 ---------- */
function openCreateCompany(): void {
  editingCompanyId.value = null
  Object.assign(companyForm, { name: '', logo: '', industry: '', scale: '', region: '', intro: '' })
  showCompanyForm.value = true
}

function openEditCompany(c: Company): void {
  editingCompanyId.value = c.id
  Object.assign(companyForm, {
    name: c.name,
    logo: c.logo || '',
    industry: c.industry || '',
    scale: c.scale || '',
    region: c.region || '',
    intro: c.intro || '',
  })
  showCompanyForm.value = true
}

async function submitCompany(): Promise<void> {
  if (!companyForm.name.trim()) {
    showToast('请填写公司名称', 'error')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    const payload = { ...companyForm, name: companyForm.name.trim() }
    if (editingCompanyId.value) {
      await updateCompany(editingCompanyId.value, payload)
    } else {
      await createCompany(payload)
    }
    showToast('保存成功', 'success')
    showCompanyForm.value = false
    load()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function removeCompany(c: Company): Promise<void> {
  try {
    await deleteCompany(c.id)
    showToast('删除成功', 'success')
    refreshAfterRemove()
  } catch {
    /* 已提示 */
  }
}

/* ---------- 评论 / 答疑 ---------- */
async function onHideComment(c: AdminCommentVO): Promise<void> {
  try {
    await hideComment(c.id)
    showToast('评论已隐藏', 'success')
    refreshAfterRemove()
  } catch {
    /* 已提示 */
  }
}

function startAnswer(q: AdminQuestionVO): void {
  answeringId.value = q.id
  answerText.value = q.answer || ''
}

function cancelAnswer(): void {
  answeringId.value = null
  answerText.value = ''
}

async function submitAnswer(q: AdminQuestionVO): Promise<void> {
  if (!answerText.value.trim()) {
    showToast('请输入回复内容', 'error')
    return
  }
  if (saving.value) return
  saving.value = true
  try {
    await answerQuestion(q.id, answerText.value.trim())
    showToast('回复成功', 'success')
    cancelAnswer()
    load()
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div class="admin-head">
      <h1 class="admin-title">后台管理</h1>
      <p class="admin-sub">运营管理用户、课程、职位与订单</p>
    </div>

    <div class="tab-bar" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        role="tab"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <div class="toolbar">
      <template v-if="activeTab === 'courses'">
        <button class="btn btn-primary btn-sm" type="button" @click="openCreateCourse">新增课程</button>
      </template>
      <template v-else-if="activeTab === 'jobs'">
        <button class="btn btn-primary btn-sm" type="button" @click="openCreateJob">新增职位</button>
      </template>
      <template v-else-if="activeTab === 'companies'">
        <button class="btn btn-primary btn-sm" type="button" @click="openCreateCompany">新增公司</button>
      </template>
    </div>

    <!-- 课程表单 -->
    <div v-if="activeTab === 'courses' && showCourseForm" class="form-panel">
      <h2 class="form-title">{{ editingCourseId ? '编辑课程' : '新增课程' }}</h2>
      <div class="form-grid">
        <div class="form-item">
          <label>课程名称</label>
          <input v-model.trim="courseForm.title" class="form-input" maxlength="100" />
        </div>
        <div class="form-item">
          <label>课程类型</label>
          <select v-model="courseForm.type" class="form-input">
            <option :value="0">理论课程</option>
            <option :value="1">实训课程</option>
            <option :value="2">认证课程</option>
          </select>
        </div>
        <div class="form-item">
          <label>课程等级</label>
          <select v-model="courseForm.level" class="form-input">
            <option :value="0">初级</option>
            <option :value="1">中级</option>
            <option :value="2">高级</option>
            <option :value="3">专业共建合作</option>
          </select>
        </div>
        <div class="form-item">
          <label>是否免费</label>
          <select v-model="courseForm.is_free" class="form-input">
            <option :value="1">免费</option>
            <option :value="0">付费</option>
          </select>
        </div>
        <div class="form-item">
          <label>价格</label>
          <input v-model.number="courseForm.price" type="number" class="form-input" min="0" step="0.01" />
        </div>
        <div class="form-item">
          <label>讲师姓名</label>
          <input v-model.trim="courseForm.teacher_name" class="form-input" maxlength="32" />
        </div>
        <div class="form-item full">
          <label>课程简介</label>
          <textarea v-model="courseForm.intro" class="form-input textarea" maxlength="2000" />
        </div>
        <div class="form-item full">
          <label>课程目标</label>
          <textarea v-model="courseForm.target" class="form-input textarea" maxlength="2000" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn" type="button" @click="showCourseForm = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="submitCourse">保存</button>
      </div>
    </div>

    <!-- 职位表单 -->
    <div v-if="activeTab === 'jobs' && showJobForm" class="form-panel">
      <h2 class="form-title">{{ editingJobId ? '编辑职位' : '新增职位' }}</h2>
      <div class="form-grid">
        <div class="form-item">
          <label>职位名称</label>
          <input v-model.trim="jobForm.title" class="form-input" maxlength="100" />
        </div>
        <div class="form-item">
          <label>所属公司</label>
          <select v-model.number="jobForm.company_id" class="form-input">
            <option :value="0" disabled>请选择公司</option>
            <option v-for="co in companyOptions" :key="co.id" :value="co.id">{{ co.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>职位分类</label>
          <select v-model.number="rootCategoryId" class="form-input" @change="onRootCategoryChange">
            <option :value="null" disabled>请选择一级分类</option>
            <option v-for="c in jobCategories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>细分方向</label>
          <select v-model.number="jobForm.category_id" class="form-input" :disabled="rootCategoryId === null">
            <option :value="null" disabled>{{ rootCategoryId === null ? '请先选择一级分类' : '请选择细分方向' }}</option>
            <option v-for="c in childCategories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="form-item">
          <label>工作城市</label>
          <input v-model.trim="jobForm.city" class="form-input" maxlength="50" />
        </div>
        <div class="form-item">
          <label>薪资下限(K)</label>
          <input v-model.number="jobForm.salary_min" type="number" class="form-input" min="0" />
        </div>
        <div class="form-item">
          <label>薪资上限(K)</label>
          <input v-model.number="jobForm.salary_max" type="number" class="form-input" min="0" />
        </div>
        <div class="form-item">
          <label>招聘人数</label>
          <input v-model.number="jobForm.headcount" type="number" class="form-input" min="1" />
        </div>
        <div class="form-item">
          <label>到期时间</label>
          <input v-model="jobForm.expire_time" type="datetime-local" class="form-input" />
        </div>
        <div class="form-item full">
          <label>详细地址</label>
          <input v-model.trim="jobForm.address" class="form-input" maxlength="255" />
        </div>
        <div class="form-item full">
          <label>职位描述</label>
          <textarea v-model="jobForm.description" class="form-input textarea" maxlength="2000" />
        </div>
        <div class="form-item full">
          <label>任职要求</label>
          <textarea v-model="jobForm.requirement" class="form-input textarea" maxlength="2000" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn" type="button" @click="showJobForm = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="submitJob">保存</button>
      </div>
    </div>

    <!-- 公司表单 -->
    <div v-if="activeTab === 'companies' && showCompanyForm" class="form-panel">
      <h2 class="form-title">{{ editingCompanyId ? '编辑公司' : '新增公司' }}</h2>
      <div class="form-grid">
        <div class="form-item">
          <label>公司名称</label>
          <input v-model.trim="companyForm.name" class="form-input" maxlength="100" />
        </div>
        <div class="form-item">
          <label>所属行业</label>
          <input v-model.trim="companyForm.industry" class="form-input" maxlength="50" placeholder="如 人工智能" />
        </div>
        <div class="form-item">
          <label>公司规模</label>
          <input v-model.trim="companyForm.scale" class="form-input" maxlength="50" placeholder="如 500-999人" />
        </div>
        <div class="form-item">
          <label>所在地区</label>
          <input v-model.trim="companyForm.region" class="form-input" maxlength="50" placeholder="如 深圳" />
        </div>
        <div class="form-item full">
          <label>LOGO 地址</label>
          <input v-model.trim="companyForm.logo" class="form-input" maxlength="255" placeholder="选填，/uploads/ 前缀" />
        </div>
        <div class="form-item full">
          <label>公司简介</label>
          <textarea v-model="companyForm.intro" class="form-input textarea" maxlength="1000" />
        </div>
      </div>
      <div class="form-actions">
        <button class="btn" type="button" @click="showCompanyForm = false">取消</button>
        <button class="btn btn-primary" type="button" :disabled="saving" @click="submitCompany">保存</button>
      </div>
    </div>

    <!-- 课程章节目录面板 -->
    <div v-if="chapterPanel.visible" class="form-panel">
      <div class="panel-head">
        <h2 class="form-title">课程目录 · {{ chapterPanel.courseTitle }}</h2>
        <button class="btn btn-sm" type="button" @click="closeChapters">关闭</button>
      </div>

      <div class="chapter-form">
        <input
          v-model.trim="chapterForm.title"
          class="form-input"
          placeholder="资源标题，如 第1讲 课程概述"
          maxlength="100"
        />
        <select v-model.number="chapterForm.resource_type" class="form-input chapter-select" aria-label="资源类型">
          <option :value="0">课件</option>
          <option :value="1">视频</option>
          <option :value="2">实验</option>
        </select>
        <input v-model.number="chapterForm.duration" type="number" class="form-input chapter-num" min="0" placeholder="时长(秒)" />
        <input v-model.number="chapterForm.sort" type="number" class="form-input chapter-num" min="0" placeholder="排序" />
        <button class="btn btn-primary btn-sm" type="button" :disabled="saving" @click="submitChapter">
          {{ editingChapterId ? '保存修改' : '添加' }}
        </button>
        <button v-if="editingChapterId" class="btn btn-sm" type="button" @click="resetChapterForm">取消编辑</button>
      </div>

      <ul v-if="chapters.length" class="chapter-manage-list">
        <li v-for="ch in chapters" :key="ch.id" class="chapter-manage-item">
          <span class="chapter-manage-type">{{ RESOURCE_TYPE_MAP.get(ch.resource_type) || '资源' }}</span>
          <span class="chapter-manage-title" :title="ch.title">{{ ch.title }}</span>
          <span class="chapter-manage-duration">{{ formatDuration(ch.duration) }}</span>
          <button class="link-btn" type="button" @click="editChapter(ch)">编辑</button>
          <button class="link-btn danger" type="button" @click="removeChapter(ch)">删除</button>
        </li>
      </ul>
      <p v-else class="chapter-empty">该课程暂无目录，添加第一条资源吧</p>
    </div>

    <div class="table-wrap">
      <div v-if="loading" class="loading-tip">加载中…</div>

      <table v-else-if="total > 0" class="data-table">
        <thead>
          <tr>
            <template v-if="activeTab === 'users'">
              <th>ID</th><th>账号</th><th>昵称</th><th>角色</th><th>状态</th><th>注册时间</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'courses'">
              <th>ID</th><th>课程名称</th><th>类型</th><th>价格</th><th>学习人数</th><th>状态</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'jobs'">
              <th>ID</th><th>职位名称</th><th>薪资</th><th>公司ID</th><th>招聘人数</th><th>状态</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'orders'">
              <th>ID</th><th>订单号</th><th>用户</th><th>课程</th><th>金额</th><th>状态</th><th>时间</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'applications'">
              <th>ID</th><th>用户</th><th>职位</th><th>状态</th><th>投递时间</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'interviews'">
              <th>ID</th><th>用户</th><th>职位</th><th>状态</th><th>创建时间</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'companies'">
              <th>ID</th><th>公司名称</th><th>行业</th><th>规模</th><th>地区</th><th>操作</th>
            </template>
            <template v-else-if="activeTab === 'comments'">
              <th>ID</th><th>课程</th><th>学员</th><th>评分</th><th>内容</th><th>状态</th><th>操作</th>
            </template>
            <template v-else>
              <th>ID</th><th>课程</th><th>学员</th><th>问题</th><th>状态</th><th>操作</th>
            </template>
          </tr>
        </thead>
        <tbody>
          <!-- 用户 -->
          <template v-if="activeTab === 'users'">
            <tr v-for="u in users" :key="u.id">
              <td>{{ u.id }}</td>
              <td>{{ u.username }}</td>
              <td>{{ u.nickname || '—' }}</td>
              <td>{{ ROLE_MAP.get(u.role) || '学员' }}</td>
              <td><span class="status" :class="u.status === USER_STATUS.BANNED ? 'off' : 'on'">{{ u.status === USER_STATUS.BANNED ? '封禁' : '正常' }}</span></td>
              <td>{{ fmtDate(u.created_at) }}</td>
              <td><button class="link-btn" type="button" @click="onToggleUser(u)">{{ u.status === USER_STATUS.BANNED ? '启用' : '封禁' }}</button></td>
            </tr>
          </template>

          <!-- 课程 -->
          <template v-else-if="activeTab === 'courses'">
            <tr v-for="c in courses" :key="c.id">
              <td>{{ c.id }}</td>
              <td class="cell-title">{{ c.title }}</td>
              <td>{{ COURSE_TYPE_MAP.get(c.type) || '—' }}</td>
              <td>{{ c.is_free === 1 ? '免费' : `¥${toNumber(c.price).toFixed(2)}` }}</td>
              <td>{{ c.student_count }}</td>
              <td><span class="status" :class="c.status === 1 ? 'on' : 'off'">{{ c.status === 1 ? '上架' : '下架' }}</span></td>
              <td>
                <button class="link-btn" type="button" @click="openChapters(c)">目录</button>
                <button class="link-btn" type="button" @click="openEditCourse(c)">编辑</button>
                <button class="link-btn" type="button" @click="onToggleCourse(c)">{{ c.status === 1 ? '下架' : '上架' }}</button>
              </td>
            </tr>
          </template>

          <!-- 职位 -->
          <template v-else-if="activeTab === 'jobs'">
            <tr v-for="j in jobs" :key="j.id">
              <td>{{ j.id }}</td>
              <td class="cell-title">{{ j.title }}</td>
              <td>{{ formatSalary(j.salary_min, j.salary_max) }}</td>
              <td>{{ j.company_id }}</td>
              <td>{{ j.headcount }}</td>
              <td><span class="status" :class="j.status === 1 ? 'on' : 'off'">{{ j.status === 1 ? '上架' : '下架' }}</span></td>
              <td>
                <button class="link-btn" type="button" @click="openEditJob(j)">编辑</button>
                <button class="link-btn" type="button" @click="onToggleJob(j)">{{ j.status === 1 ? '下架' : '上架' }}</button>
              </td>
            </tr>
          </template>

          <!-- 订单 -->
          <template v-else-if="activeTab === 'orders'">
            <tr v-for="o in orders" :key="o.id">
              <td>{{ o.id }}</td>
              <td class="cell-mono">{{ o.order_no }}</td>
              <td>{{ o.user_id }}</td>
              <td>{{ o.course_id }}</td>
              <td>¥{{ toNumber(o.amount).toFixed(2) }}</td>
              <td>{{ ORDER_STATUS_MAP.get(o.status) || '—' }}</td>
              <td>{{ fmtDate(o.created_at) }}</td>
              <td>
                <button v-if="o.status === 1" class="link-btn" type="button" @click="onRefund(o)">退款</button>
                <span v-else>—</span>
              </td>
            </tr>
          </template>

          <!-- 申请 -->
          <template v-else-if="activeTab === 'applications'">
            <tr v-for="a in applications" :key="a.id">
              <td>{{ a.id }}</td>
              <td>{{ a.user_id }}</td>
              <td>{{ a.job_id }}</td>
              <td>{{ APPLICATION_STATUS_MAP.get(a.status) || '—' }}</td>
              <td>{{ fmtDate(a.created_at) }}</td>
              <td class="actions-cell">
                <button v-if="a.status === 0" class="link-btn" type="button" @click="onApplicationStatus(a, 1)">标记已查看</button>
                <button v-if="a.status !== 2" class="link-btn" type="button" @click="onApplicationStatus(a, 2)">通过</button>
                <button v-if="a.status !== 3" class="link-btn danger" type="button" @click="onApplicationStatus(a, 3)">拒绝</button>
              </td>
            </tr>
          </template>

          <!-- 面试 -->
          <template v-else-if="activeTab === 'interviews'">
            <tr v-for="it in interviews" :key="it.id">
              <td>{{ it.id }}</td>
              <td>{{ it.user_id }}</td>
              <td>{{ it.job_id }}</td>
              <td>{{ it.status === 0 ? '待进行' : it.status === 1 ? '进行中' : '已完成' }}</td>
              <td>{{ fmtDate(it.created_at) }}</td>
              <td class="actions-cell">
                <button v-if="it.status !== 2" class="link-btn" type="button" @click="onInterviewStatus(it, it.status + 1)">推进</button>
                <span v-else>—</span>
              </td>
            </tr>
          </template>

          <!-- 公司 -->
          <template v-else-if="activeTab === 'companies'">
            <tr v-for="co in companies" :key="co.id">
              <td>{{ co.id }}</td>
              <td class="cell-title">{{ co.name }}</td>
              <td>{{ co.industry || '—' }}</td>
              <td>{{ co.scale || '—' }}</td>
              <td>{{ co.region || '—' }}</td>
              <td class="actions-cell">
                <button class="link-btn" type="button" @click="openEditCompany(co)">编辑</button>
                <button class="link-btn danger" type="button" @click="removeCompany(co)">删除</button>
              </td>
            </tr>
          </template>

          <!-- 评论 -->
          <template v-else-if="activeTab === 'comments'">
            <tr v-for="c in adminComments" :key="c.id">
              <td>{{ c.id }}</td>
              <td class="cell-title">{{ c.course_title || `课程#${c.course_id}` }}</td>
              <td>{{ c.nickname || `用户#${c.user_id}` }}</td>
              <td>{{ c.score }}</td>
              <td class="cell-title">{{ c.content || '—' }}</td>
              <td>{{ c.status === 1 ? '正常' : '已隐藏' }}</td>
              <td>
                <button v-if="c.status === 1" class="link-btn danger" type="button" @click="onHideComment(c)">隐藏</button>
                <span v-else>—</span>
              </td>
            </tr>
          </template>

          <!-- 答疑 -->
          <template v-else>
            <template v-for="q in adminQuestions" :key="q.id">
              <tr>
                <td>{{ q.id }}</td>
                <td class="cell-title">{{ q.course_title || `课程#${q.course_id}` }}</td>
                <td>{{ q.nickname || `用户#${q.user_id}` }}</td>
                <td class="cell-title">{{ q.question }}</td>
                <td>{{ q.status === 1 ? '已回复' : '待回复' }}</td>
                <td>
                  <button v-if="answeringId !== q.id" class="link-btn" type="button" @click="startAnswer(q)">
                    {{ q.status === 1 ? '修改回复' : '回复' }}
                  </button>
                  <span v-else>—</span>
                </td>
              </tr>
              <tr v-if="answeringId === q.id">
                <td colspan="6" class="answer-row">
                  <textarea v-model="answerText" class="form-input textarea" placeholder="输入回复内容…" maxlength="1000" />
                  <div class="form-actions">
                    <button class="btn btn-sm" type="button" @click="cancelAnswer">取消</button>
                    <button class="btn btn-primary btn-sm" type="button" :disabled="saving" @click="submitAnswer(q)">提交回复</button>
                  </div>
                </td>
              </tr>
            </template>
          </template>
        </tbody>
      </table>

      <div v-else class="empty-tip">
        <span class="empty-icon" aria-hidden="true">空</span>
        <p>暂无数据</p>
      </div>
    </div>

    <div class="pager">
      <button class="btn btn-sm" type="button" :disabled="pageNum <= 1" @click="changePage(pageNum - 1)">上一页</button>
      <span class="page-info">{{ pageNum }} / {{ Math.max(1, Math.ceil(total / PAGE_SIZE)) }}</span>
      <button class="btn btn-sm" type="button" :disabled="pageNum >= Math.ceil(total / PAGE_SIZE)" @click="changePage(pageNum + 1)">下一页</button>
    </div>
  </div>
</template>

<style scoped>
.admin-page {
  min-width: 0;
}

.admin-head {
  margin-bottom: var(--space-4);
}

.admin-title {
  font-size: 22px;
  font-weight: 700;
}

.admin-sub {
  margin-top: 2px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.tab-bar {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  padding: 4px;
  margin-bottom: var(--space-4);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.tab {
  padding: 8px 18px;
  border-radius: var(--radius-md);
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: all var(--dur-fast) ease;
}

.tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.toolbar {
  display: flex;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.form-panel {
  padding: var(--space-5);
  margin-bottom: var(--space-4);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.form-title {
  margin-bottom: var(--space-4);
  font-size: 16px;
  font-weight: 600;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--space-4);
}

.form-grid .full {
  grid-column: 1 / -1;
}

.textarea {
  height: auto;
  min-height: 80px;
  padding: 10px 14px;
  resize: vertical;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-2);
  margin-top: var(--space-4);
}

.table-wrap {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.loading-tip {
  padding: var(--space-6);
  text-align: center;
  color: var(--color-text-tertiary);
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th,
.data-table td {
  padding: 12px 14px;
  text-align: left;
  border-bottom: 1px solid var(--color-border);
  white-space: nowrap;
}

.data-table th {
  padding: 11px 14px;
  background: #fafbfc;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
  color: var(--color-text-tertiary);
}

.data-table tbody tr {
  transition: background var(--dur-fast) ease;
}

.data-table tbody tr:hover td {
  background: #fafbfc;
}

.data-table tr:last-child td {
  border-bottom: none;
}

.cell-title {
  max-width: 240px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.cell-mono {
  font-family: var(--font-num);
  font-size: 12px;
}

.status {
  display: inline-flex;
  align-items: center;
  padding: 1px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 11.5px;
  line-height: 18px;
}

.status.on {
  border-color: rgba(31, 122, 69, 0.22);
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status.off {
  border-color: rgba(179, 38, 30, 0.22);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.link-btn {
  margin-right: 10px;
  font-size: 13px;
  color: var(--color-primary);
}

.link-btn.danger {
  color: var(--color-danger);
}

.link-btn:hover {
  text-decoration: underline;
}

.actions-cell {
  white-space: nowrap;
}

/* ---------- 章节目录面板 ---------- */
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: var(--space-4);
}

.panel-head .form-title {
  margin-bottom: 0;
}

.chapter-form {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
}

.chapter-form .form-input {
  flex: 1;
  min-width: 160px;
  height: 38px;
}

.chapter-select {
  flex: 0 0 100px;
}

.chapter-num {
  flex: 0 0 110px;
}

.chapter-manage-list {
  display: flex;
  flex-direction: column;
  border-top: 1px solid var(--color-border);
}

.chapter-manage-item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: 10px 0;
  border-bottom: 1px dashed var(--color-border);
  font-size: 13px;
}

.chapter-manage-type {
  flex-shrink: 0;
  min-width: 42px;
  padding: 1px 8px;
  border-radius: 4px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 11px;
  text-align: center;
}

.chapter-manage-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-manage-duration {
  flex-shrink: 0;
  font-family: var(--font-num);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.chapter-empty {
  padding: var(--space-5);
  text-align: center;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* ---------- 答疑回复展开行 ---------- */
.answer-row {
  padding: var(--space-4) !important;
  background: var(--color-bg);
}

.answer-row .textarea {
  margin-bottom: var(--space-3);
}

.pager {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-3);
  padding: var(--space-5) 0;
}

.page-info {
  font-size: 13px;
  color: var(--color-text-tertiary);
  font-family: var(--font-num);
}

@media (max-width: 720px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .data-table {
    display: block;
    overflow-x: auto;
  }
}
</style>
