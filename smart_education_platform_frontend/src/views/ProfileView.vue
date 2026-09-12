<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile, updateAvatar } from '@/api/user'
import { getMyCourses } from '@/api/course'
import { getMyOrders } from '@/api/order'
import { getMyApplications, getMyInterviews } from '@/api/job'
import { authState, setUserInfo } from '@/stores/auth'
import { showToast } from '@/composables/toast'
import Pagination from '@/components/Pagination.vue'
import {
  APPLICATION_STATUS,
  APPLICATION_STATUS_MAP,
  COURSE_TYPE_MAP,
  INTERVIEW_STATUS,
  INTERVIEW_STATUS_MAP,
  ORDER_STATUS,
  ORDER_STATUS_MAP,
  ROLE_MAP,
  formatSalary,
  toNumber,
  type MyCourseVO,
  type MyInterviewVO,
  type MyJobApplicationVO,
  type MyOrderVO,
  type UserVO,
} from '@/types/api'

type Tab = 'profile' | 'courses' | 'orders' | 'applications' | 'interviews'

const PAGE_SIZE = 10

const tabs: { key: Tab; label: string }[] = [
  { key: 'profile', label: '个人资料' },
  { key: 'courses', label: '我的课程' },
  { key: 'orders', label: '我的订单' },
  { key: 'applications', label: '我的投递' },
  { key: 'interviews', label: '我的面试' },
]

const activeTab = ref<Tab>('profile')

/* ---------- 个人资料 ---------- */
const user = ref<UserVO | null>(null)
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

function triggerUpload(): void {
  fileInput.value?.click()
}

const form = reactive({
  nickname: '',
  real_name: '',
})

async function fetchProfile(): Promise<void> {
  loading.value = true
  try {
    const data = await getProfile()
    user.value = data
    form.nickname = data.nickname || ''
    form.real_name = data.real_name || ''
    setUserInfo(data)
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

async function onSave(): Promise<void> {
  if (saving.value) return
  saving.value = true
  try {
    const data = await updateProfile({
      nickname: form.nickname.trim(),
      real_name: form.real_name.trim(),
    })
    user.value = data
    setUserInfo(data)
    showToast('资料已更新', 'success')
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function onAvatarChange(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || uploading.value) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    showToast('头像仅支持 JPG/PNG', 'error')
    input.value = ''
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    showToast('头像大小不能超过 2MB', 'error')
    input.value = ''
    return
  }
  uploading.value = true
  try {
    const avatar = await updateAvatar(file)
    if (user.value) user.value.avatar = avatar
    if (authState.userInfo) authState.userInfo.avatar = avatar
    showToast('头像已更新', 'success')
  } catch {
    /* 已提示 */
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const roleText = (role?: number) => ROLE_MAP.get(role ?? 0) || '学员'

/* ---------- 我的记录（课程 / 订单 / 投递 / 面试） ---------- */
const listLoading = ref(false)
/** 请求序号：丢弃过期响应，避免快速切换 tab 时旧数据覆盖新数据 */
let loadSeq = 0

const myCourses = ref<MyCourseVO[]>([])
const myOrders = ref<MyOrderVO[]>([])
const myApplications = ref<MyJobApplicationVO[]>([])
const myInterviews = ref<MyInterviewVO[]>([])
const total = ref(0)
const pageNum = ref(1)

const emptyText = computed(() => {
  switch (activeTab.value) {
    case 'courses':
      return '还没有报名任何课程'
    case 'orders':
      return '还没有课程订单'
    case 'applications':
      return '还没有投递过职位'
    default:
      return '还没有申请过数字人面试'
  }
})

async function loadList(): Promise<void> {
  const tab = activeTab.value
  if (tab === 'profile') return
  const seq = ++loadSeq
  listLoading.value = true
  try {
    if (tab === 'courses') {
      const res = await getMyCourses(pageNum.value, PAGE_SIZE)
      if (seq !== loadSeq) return
      myCourses.value = res.records
      total.value = res.total
    } else if (tab === 'orders') {
      const res = await getMyOrders(pageNum.value, PAGE_SIZE)
      if (seq !== loadSeq) return
      myOrders.value = res.records
      total.value = res.total
    } else if (tab === 'applications') {
      const res = await getMyApplications(pageNum.value, PAGE_SIZE)
      if (seq !== loadSeq) return
      myApplications.value = res.records
      total.value = res.total
    } else {
      const res = await getMyInterviews(pageNum.value, PAGE_SIZE)
      if (seq !== loadSeq) return
      myInterviews.value = res.records
      total.value = res.total
    }
  } catch {
    // 请求失败时清空列表与总数，避免残留上一个 tab 的数据渲染出"有表头无数据"的假列表
    if (seq === loadSeq) {
      total.value = 0
      clearCurrentList()
    }
  } finally {
    if (seq === loadSeq) listLoading.value = false
  }
}

function clearCurrentList(): void {
  switch (activeTab.value) {
    case 'courses':
      myCourses.value = []
      break
    case 'orders':
      myOrders.value = []
      break
    case 'applications':
      myApplications.value = []
      break
    default:
      myInterviews.value = []
  }
}

function switchTab(tab: Tab): void {
  if (activeTab.value === tab) return
  activeTab.value = tab
  if (tab === 'profile') return
  pageNum.value = 1
  void loadList()
}

function changePage(next: number): void {
  pageNum.value = next
  void loadList()
}

/* ---------- 展示辅助 ---------- */
function fmtDate(v?: string | null): string {
  return v ? v.slice(0, 10) : '—'
}

function fmtDateTime(v?: string | null): string {
  return v ? v.slice(0, 16).replace('T', ' ') : '—'
}

function courseTypeText(type: number | null): string {
  return type == null ? '' : COURSE_TYPE_MAP.get(type) || ''
}

/** 状态标签色调：待处理类用 warn，进行中/已查看用 info，成功用 ok，失败用 danger，其余置灰 */
function orderTone(status: number): string {
  if (status === ORDER_STATUS.PENDING) return 'warn'
  if (status === ORDER_STATUS.PAID) return 'ok'
  return 'muted'
}

function applicationTone(status: number): string {
  if (status === APPLICATION_STATUS.PENDING) return 'warn'
  if (status === APPLICATION_STATUS.VIEWED) return 'info'
  if (status === APPLICATION_STATUS.PASSED) return 'ok'
  return 'danger'
}

function interviewTone(status: number): string {
  if (status === INTERVIEW_STATUS.TODO) return 'warn'
  if (status === INTERVIEW_STATUS.DOING) return 'info'
  return 'ok'
}

onMounted(fetchProfile)
</script>

<template>
  <div class="profile-page">
    <div class="page-head">
      <h1 class="page-title">个人中心</h1>
      <p class="page-sub">管理个人资料，查看报名、订单与求职记录</p>
    </div>

    <div class="tab-bar" role="tablist">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        type="button"
        role="tab"
        class="tab"
        :class="{ active: activeTab === tab.key }"
        :aria-selected="activeTab === tab.key"
        @click="switchTab(tab.key)"
      >
        {{ tab.label }}
      </button>
    </div>

    <!-- ============ 个人资料 ============ -->
    <template v-if="activeTab === 'profile'">
      <div v-if="loading" class="skeleton skel-card" />

      <div v-else-if="user" class="profile-card">
        <div class="profile-head">
          <button class="avatar-btn" type="button" :disabled="uploading" title="点击更换头像" @click="triggerUpload">
            <img v-if="user.avatar" :src="user.avatar ?? ''" :alt="user.nickname || '头像'" class="avatar" />
            <span v-else class="avatar avatar-fallback">{{ (user.nickname || user.username).charAt(0).toUpperCase() }}</span>
            <span class="avatar-mask" aria-hidden="true">{{ uploading ? '上传中…' : '更换' }}</span>
          </button>
          <input ref="fileInput" type="file" accept="image/png,image/jpeg" class="visually-hidden" @change="onAvatarChange" />

          <div class="profile-title">
            <h2 class="name">{{ user.nickname || user.username }}</h2>
            <p class="account">@{{ user.username }} · {{ roleText(user.role) }}</p>
          </div>
        </div>

        <div class="profile-body">
          <div class="form-grid">
            <div class="form-item">
              <label for="pf-nickname">昵称</label>
              <input id="pf-nickname" v-model.trim="form.nickname" class="form-input" maxlength="32" placeholder="设置昵称" />
            </div>
            <div class="form-item">
              <label for="pf-realname">姓名</label>
              <input id="pf-realname" v-model.trim="form.real_name" class="form-input" maxlength="32" placeholder="真实姓名" />
            </div>
          </div>

          <dl class="info-list">
            <div class="info-item">
              <dt>邮箱</dt>
              <dd>{{ user.email || '未绑定' }}</dd>
            </div>
            <div class="info-item">
              <dt>手机号</dt>
              <dd>{{ user.phone || '未绑定' }}</dd>
            </div>
            <div class="info-item">
              <dt>注册时间</dt>
              <dd>{{ fmtDate(user.created_at) }}</dd>
            </div>
          </dl>

          <div class="actions">
            <button class="btn btn-primary" type="button" :disabled="saving" @click="onSave">
              {{ saving ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </div>
      </div>
    </template>

    <!-- ============ 我的课程 / 订单 / 投递 / 面试 ============ -->
    <template v-else>
      <div v-if="listLoading" class="record-skeleton">
        <div v-for="n in 3" :key="n" class="skeleton skel-row" />
      </div>

      <template v-else-if="total > 0">
        <!-- 我的课程 -->
        <div v-if="activeTab === 'courses'" class="record-list">
          <article v-for="c in myCourses" :key="c.id" class="record">
            <div class="record-main">
              <div class="record-top">
                <h3 class="record-title">{{ c.title || '课程已下架' }}</h3>
              </div>
              <p class="record-meta">
                <span v-if="c.teacher_name">{{ c.teacher_name }}</span>
                <span v-if="courseTypeText(c.type)">{{ courseTypeText(c.type) }}</span>
                <span>报名于 {{ fmtDateTime(c.enroll_time) }}</span>
              </p>
              <div class="progress-line">
                <span class="progress-track" aria-hidden="true">
                  <i :style="{ width: `${c.progress ?? 0}%` }" />
                </span>
                <span class="progress-text">
                  学习进度 {{ c.progress ?? 0 }}%（{{ c.finish_count ?? 0 }}/{{ c.total_count ?? 0 }}）
                </span>
              </div>
            </div>
            <div class="record-actions">
              <RouterLink v-if="c.title" class="btn btn-sm" :to="`/course/detail/${c.course_id}`">去学习</RouterLink>
              <span v-else class="record-gone">已下架</span>
            </div>
          </article>
        </div>

        <!-- 我的订单 -->
        <div v-else-if="activeTab === 'orders'" class="record-list">
          <article v-for="o in myOrders" :key="o.id" class="record">
            <div class="record-main">
              <div class="record-top">
                <h3 class="record-title">{{ o.course_title || '课程已下架' }}</h3>
                <span class="pill" :class="orderTone(o.status)">{{ ORDER_STATUS_MAP.get(o.status) || '—' }}</span>
              </div>
              <p class="record-meta">
                <span class="record-mono">{{ o.order_no }}</span>
                <span>下单于 {{ fmtDateTime(o.created_at) }}</span>
                <span v-if="o.pay_time">支付于 {{ fmtDateTime(o.pay_time) }}</span>
              </p>
            </div>
            <div class="record-actions">
              <span class="record-amount">¥{{ toNumber(o.amount).toFixed(2) }}</span>
              <RouterLink
                v-if="o.status === ORDER_STATUS.PENDING"
                class="btn btn-primary btn-sm"
                :to="`/course/detail/${o.course_id}`"
              >
                去支付
              </RouterLink>
              <RouterLink v-else-if="o.course_title" class="btn btn-sm" :to="`/course/detail/${o.course_id}`">
                查看课程
              </RouterLink>
              <span v-else class="record-gone">已下架</span>
            </div>
          </article>
        </div>

        <!-- 我的投递 -->
        <div v-else-if="activeTab === 'applications'" class="record-list">
          <article v-for="a in myApplications" :key="a.id" class="record">
            <div class="record-main">
              <div class="record-top">
                <h3 class="record-title">{{ a.job_title || '职位已下架' }}</h3>
                <span class="pill" :class="applicationTone(a.status)">
                  {{ APPLICATION_STATUS_MAP.get(a.status) || '—' }}
                </span>
              </div>
              <p class="record-meta">
                <span v-if="a.company_name">{{ a.company_name }}</span>
                <span v-if="a.city">{{ a.city }}</span>
                <span>投递于 {{ fmtDateTime(a.created_at) }}</span>
              </p>
            </div>
            <div class="record-actions">
              <span class="record-amount record-salary">{{ formatSalary(a.salary_min, a.salary_max) }}</span>
              <RouterLink v-if="a.job_title" class="btn btn-sm" :to="`/job/detail/${a.job_id}`">查看职位</RouterLink>
              <span v-else class="record-gone">已下架</span>
            </div>
          </article>
        </div>

        <!-- 我的面试 -->
        <div v-else class="record-list">
          <article v-for="iv in myInterviews" :key="iv.id" class="record">
            <div class="record-main">
              <div class="record-top">
                <h3 class="record-title">{{ iv.job_title || '职位已下架' }}</h3>
                <span class="pill" :class="interviewTone(iv.status)">
                  {{ INTERVIEW_STATUS_MAP.get(iv.status) || '—' }}
                </span>
              </div>
              <p class="record-meta">
                <span v-if="iv.company_name">{{ iv.company_name }}</span>
                <span>申请于 {{ fmtDateTime(iv.created_at) }}</span>
                <span v-if="iv.interview_time">面试时间 {{ fmtDateTime(iv.interview_time) }}</span>
              </p>
            </div>
            <div class="record-actions">
              <RouterLink v-if="iv.job_title" class="btn btn-sm" :to="`/job/detail/${iv.job_id}`">查看职位</RouterLink>
              <span v-else class="record-gone">已下架</span>
            </div>
          </article>
        </div>

        <Pagination :current="pageNum" :total="total" :size="PAGE_SIZE" @change="changePage" />
      </template>

      <div v-else class="empty-tip">
        <span class="empty-icon" aria-hidden="true">空</span>
        <p>{{ emptyText }}</p>
        <RouterLink v-if="activeTab === 'courses'" class="btn btn-primary btn-sm" to="/course/0">去课程中心</RouterLink>
        <RouterLink v-else class="btn btn-primary btn-sm" to="/job">去看看职位</RouterLink>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  min-width: 0;
  max-width: 820px;
}

.page-head {
  margin-bottom: var(--space-4);
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: var(--tracking-tight);
}

.page-sub {
  margin-top: 2px;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* ---------- Tab ---------- */
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
  transition: color var(--dur-fast) ease, background var(--dur-fast) ease;
}

.tab:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

/* ---------- 资料卡 ---------- */
.skel-card {
  height: 360px;
  border-radius: var(--radius-md);
}

.profile-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
  background: linear-gradient(180deg, var(--color-primary-soft) 0%, transparent 100%);
}

.avatar-btn {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(23, 35, 61, 0.5);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity var(--dur-fast) ease;
}

.avatar-btn:hover .avatar-mask {
  opacity: 1;
}

.profile-title {
  min-width: 0;
}

.name {
  font-size: 22px;
  font-weight: 700;
}

.account {
  margin-top: 2px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.profile-body {
  padding: var(--space-5) var(--space-6) var(--space-6);
}

/* 昵称与姓名上下排列，标签与输入框左对齐同宽 */
.form-grid {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.info-list {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px dashed var(--color-border);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-2) 0;
}

.info-item dt {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.info-item dd {
  margin: 0;
  font-size: 14px;
  color: var(--color-text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-5);
}

/* ---------- 记录列表 ---------- */
.record-skeleton {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skel-row {
  height: 96px;
  border-radius: var(--radius-md);
}

.record-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.record {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  transition: border-color var(--dur-base) ease, box-shadow var(--dur-base) ease;
}

.record:hover {
  border-color: var(--color-primary-border);
  box-shadow: var(--shadow-md);
}

.record-main {
  flex: 1;
  min-width: 0;
}

.record-top {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.record-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.4;
}

.record-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 4px;
  font-size: 12.5px;
  color: var(--color-text-tertiary);
}

.record-meta span + span::before {
  content: '·';
  margin-right: 6px;
}

.record-mono {
  font-family: var(--font-num);
}

.record-actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  flex-shrink: 0;
}

.record-amount {
  font-family: var(--font-num);
  font-size: 15px;
  font-weight: 700;
  color: var(--color-primary);
}

.record-salary {
  font-size: 14px;
}

.record-gone {
  font-size: 12.5px;
  color: var(--color-text-tertiary);
}

/* 学习进度 */
.progress-line {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.progress-track {
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--color-bg);
  overflow: hidden;
}

.progress-track i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--color-primary);
  transition: width var(--dur-base) ease;
}

.progress-text {
  flex-shrink: 0;
  font-family: var(--font-num);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

/* 状态标签 */
.pill {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  padding: 1px 9px;
  border: 1px solid transparent;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  line-height: 1.7;
}

.pill.ok {
  border-color: rgba(31, 122, 69, 0.22);
  background: var(--color-success-soft);
  color: var(--color-success);
}

.pill.warn {
  border-color: rgba(138, 95, 0, 0.22);
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.pill.info {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.pill.danger {
  border-color: rgba(179, 38, 30, 0.22);
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.pill.muted {
  border-color: var(--color-border);
  background: var(--color-bg);
  color: var(--color-text-tertiary);
}

@media (max-width: 560px) {
  .profile-head {
    flex-direction: column;
    text-align: center;
  }

  .record {
    flex-direction: column;
    align-items: stretch;
  }

  .record-actions {
    justify-content: space-between;
  }
}
</style>
