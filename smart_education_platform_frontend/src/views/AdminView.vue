<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
} from '@/api/admin'
import { showToast } from '@/composables/toast'
import {
  APPLICATION_STATUS_MAP,
  COURSE_TYPE_MAP,
  ORDER_STATUS_MAP,
  ROLE_MAP,
  USER_STATUS,
  formatSalary,
  toNumber,
  type AdminCourseParams,
  type AdminJobParams,
  type AiInterview,
  type Course,
  type Job,
  type JobApplication,
  type OrderInfo,
  type UserVO,
} from '@/types/api'

type Tab = 'users' | 'courses' | 'jobs' | 'orders' | 'applications' | 'interviews'

const PAGE_SIZE = 10

const activeTab = ref<Tab>('users')
const loading = ref(false)

const users = ref<UserVO[]>([])
const courses = ref<Course[]>([])
const jobs = ref<Job[]>([])
const orders = ref<OrderInfo[]>([])
const applications = ref<JobApplication[]>([])
const interviews = ref<AiInterview[]>([])

const total = ref(0)
const pageNum = ref(1)

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
]

async function load(): Promise<void> {
  loading.value = true
  try {
    const params = { page_num: pageNum.value, page_size: PAGE_SIZE }
    switch (activeTab.value) {
      case 'users': {
        const data = await getAdminUsers(params)
        users.value = data.records || []
        total.value = data.total || 0
        break
      }
      case 'courses': {
        const data = await getAdminCourses(params)
        courses.value = data.records || []
        total.value = data.total || 0
        break
      }
      case 'jobs': {
        const data = await getAdminJobs(params)
        jobs.value = data.records || []
        total.value = data.total || 0
        break
      }
      case 'orders': {
        const data = await getAdminOrders(params)
        orders.value = data.records || []
        total.value = data.total || 0
        break
      }
      case 'applications': {
        const data = await getAdminApplications(params)
        applications.value = data.records || []
        total.value = data.total || 0
        break
      }
      case 'interviews': {
        const data = await getAdminInterviews(params)
        interviews.value = data.records || []
        total.value = data.total || 0
        break
      }
    }
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

function switchTab(tab: Tab): void {
  activeTab.value = tab
  pageNum.value = 1
  showCourseForm.value = false
  showJobForm.value = false
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
function openCreateJob(): void {
  editingJobId.value = null
  Object.assign(jobForm, { title: '', city: '', address: '', salary_min: null, salary_max: null, headcount: 1, company_id: 0, category_id: null, expire_time: '', description: '', requirement: '' })
  showJobForm.value = true
}

function openEditJob(job: Job): void {
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
}

async function submitJob(): Promise<void> {
  if (!jobForm.title.trim() || !jobForm.company_id) {
    showToast('请填写职位名称与公司ID', 'error')
    return
  }
  const payload: AdminJobParams = {
    ...jobForm,
    title: jobForm.title.trim(),
    expire_time: jobForm.expire_time ? jobForm.expire_time.replace('T', ' ') + ':00' : null,
  }
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
        <button class="btn btn-primary" type="button" @click="submitCourse">保存</button>
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
          <label>公司ID</label>
          <input v-model.number="jobForm.company_id" type="number" class="form-input" min="1" />
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
        <button class="btn btn-primary" type="button" @click="submitJob">保存</button>
      </div>
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
            <template v-else>
              <th>ID</th><th>用户</th><th>职位</th><th>状态</th><th>创建时间</th><th>操作</th>
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
          <template v-else>
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
  background: var(--color-bg);
  font-weight: 600;
  color: var(--color-text-secondary);
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
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.status.on {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.status.off {
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
