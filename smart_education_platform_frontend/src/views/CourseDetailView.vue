<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getCourseChapters,
  getCourseDetail,
  getCourseComments,
  addCourseComment,
  getCourseQuestions,
  addCourseQuestion,
  toggleCourseCollect,
  enrollCourse,
  finishChapter,
} from '@/api/course'
import { isLoggedIn } from '@/stores/auth'
import { showToast } from '@/composables/toast'
import { payOrder } from '@/api/order'
import {
  COURSE_LEVEL_MAP,
  COURSE_TYPE_MAP,
  PAY_TYPE_OPTIONS,
  RESOURCE_TYPE_MAP,
  formatDuration,
  toNumber,
  type CourseChapterVO,
  type CourseCommentVO,
  type CourseDetailVO,
  type CourseQuestionVO,
} from '@/types/api'

const route = useRoute()
const router = useRouter()

const course = ref<CourseDetailVO | null>(null)
const chapters = ref<CourseChapterVO[]>([])
const comments = ref<CourseCommentVO[]>([])
const questions = ref<CourseQuestionVO[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)

const loading = ref(true)
const collectLoading = ref(false)
const enrollLoading = ref(false)

/** 支付弹层：收费课程下单后唤起 */
const payDialog = reactive({
  visible: false,
  orderNo: '',
  amount: 0,
  payType: 0,
  loading: false,
})
const activeTab = ref<'intro' | 'chapters' | 'comments' | 'questions'>('intro')

const commentForm = reactive({ score: 5, content: '' })
const commentSubmitting = ref(false)
const commentsLoading = ref(false)
const questionText = ref('')
const questionSubmitting = ref(false)
const commentsLoaded = ref(false)
const questionsLoaded = ref(false)

const courseId = computed(() => Number(route.params.id))
const moduleName = computed(() => COURSE_TYPE_MAP.get(course.value?.type ?? 0) || '课程中心')
const coverText = computed(() => (course.value?.title || '课').trim().charAt(0).toUpperCase())

/** 详情页封面用同色系深色面板，与列表页的浅色卡片形成层级对比 */
const HUE_BY_TYPE: Record<number, number> = { 0: 212, 1: 176, 2: 28 }

const coverStyle = computed(() => {
  const hue = HUE_BY_TYPE[course.value?.type ?? 0] ?? 212
  return {
    background: `linear-gradient(150deg, hsl(${hue} 34% 31%) 0%, hsl(${hue} 42% 19%) 100%)`,
  }
})
const levelLabel = computed(() =>
  course.value ? COURSE_LEVEL_MAP.get(course.value.level) || `Lv.${course.value.level}` : '',
)
const ratingText = computed(() => {
  const n = course.value ? toNumber(course.value.score) : 0
  return n > 0 ? n.toFixed(1) : '暂无'
})
const priceText = computed(() =>
  course.value?.is_free === 1 ? '免费' : `¥${course.value ? toNumber(course.value.price).toFixed(2) : '0.00'}`,
)

interface ResourceChip {
  label: string
  count: number
  key: string
}

const resourceChips = computed<ResourceChip[]>(() => {
  const c = course.value
  if (!c) return []
  const list: ResourceChip[] = []
  if (c.courseware_count > 0) list.push({ label: '课件', count: c.courseware_count, key: 'slide' })
  if (c.video_count > 0) list.push({ label: '视频', count: c.video_count, key: 'video' })
  if (c.lab_count > 0) list.push({ label: '实验', count: c.lab_count, key: 'lab' })
  return list
})

function typeText(type: number): string {
  return RESOURCE_TYPE_MAP.get(type) || '资源'
}

async function fetchDetail(): Promise<void> {
  const detail = await getCourseDetail(courseId.value)
  course.value = detail
}

async function fetchChapters(): Promise<void> {
  chapters.value = await getCourseChapters(courseId.value)
}

async function fetchAll(): Promise<void> {
  loading.value = true
  activeTab.value = 'intro'
  try {
    const [detail, list] = await Promise.all([fetchDetail(), fetchChapters()])
    void detail
    void list
    commentsLoaded.value = false
    questionsLoaded.value = false
    comments.value = []
    questions.value = []
  } catch {
    router.replace('/course/0')
  } finally {
    loading.value = false
  }
}

async function loadComments(reset = false): Promise<void> {
  if (reset) {
    commentPage.value = 1
    comments.value = []
  }
  const data = await getCourseComments(courseId.value, commentPage.value, 20)
  comments.value = comments.value.concat(data.records || [])
  commentTotal.value = data.total || 0
  commentsLoaded.value = true
}

async function loadQuestions(): Promise<void> {
  questions.value = await getCourseQuestions(courseId.value)
  questionsLoaded.value = true
}

/** 加载更多评论：失败时回滚页码，避免该页数据永久丢失 */
async function loadMoreComments(): Promise<void> {
  if (commentsLoading.value) return
  commentsLoading.value = true
  commentPage.value += 1
  try {
    await loadComments()
  } catch {
    commentPage.value -= 1
  } finally {
    commentsLoading.value = false
  }
}

function onTabChange(tab: 'intro' | 'chapters' | 'comments' | 'questions'): void {
  activeTab.value = tab
  if (tab === 'comments' && !commentsLoaded.value) {
    loadComments(true).catch(() => {})
  }
  if (tab === 'questions' && !questionsLoaded.value) {
    loadQuestions().catch(() => {})
  }
}

function requireLogin(): boolean {
  if (isLoggedIn()) return true
  showToast('请先登录', 'info')
  router.push({ path: '/login', query: { redirect: route.fullPath } })
  return false
}

async function onCollect(): Promise<void> {
  if (!requireLogin() || collectLoading.value) return
  collectLoading.value = true
  try {
    const collected = await toggleCourseCollect(courseId.value)
    if (course.value) course.value.is_collected = collected
    showToast(collected ? '已收藏' : '已取消收藏', 'success')
  } catch {
    /* 已提示 */
  } finally {
    collectLoading.value = false
  }
}

async function onEnroll(): Promise<void> {
  if (!requireLogin() || enrollLoading.value) return
  enrollLoading.value = true
  try {
    const result = await enrollCourse(courseId.value)
    if (result.enrolled) {
      showToast('报名成功，开始学习吧', 'success')
      await fetchDetail()
    } else if (result.order_no) {
      // 收费课程：唤起支付弹层，由用户选择支付方式后完成支付
      openPayDialog(result.order_no)
    }
  } catch {
    /* 已提示 */
  } finally {
    enrollLoading.value = false
  }
}

function openPayDialog(orderNo: string): void {
  payDialog.orderNo = orderNo
  payDialog.amount = toNumber(course.value?.price)
  payDialog.payType = 0
  payDialog.visible = true
}

function closePayDialog(): void {
  if (payDialog.loading) return
  payDialog.visible = false
}

async function confirmPay(): Promise<void> {
  if (payDialog.loading) return
  payDialog.loading = true
  try {
    // 课程设计简化：点击支付即视为支付成功（不接入真实支付渠道）
    await payOrder(payDialog.orderNo, payDialog.payType)
    showToast('支付成功，课程已开通', 'success')
    payDialog.visible = false
    await fetchDetail()
  } catch {
    /* 已提示 */
  } finally {
    payDialog.loading = false
  }
}

async function submitComment(): Promise<void> {
  if (!requireLogin() || commentSubmitting.value) return
  if (!commentForm.content.trim()) {
    showToast('请填写评论内容', 'error')
    return
  }
  commentSubmitting.value = true
  try {
    await addCourseComment(courseId.value, {
      score: commentForm.score,
      content: commentForm.content.trim(),
    })
    showToast('评价成功', 'success')
    commentForm.content = ''
    await loadComments(true)
    await fetchDetail()
  } catch {
    /* 已提示 */
  } finally {
    commentSubmitting.value = false
  }
}

async function submitQuestion(): Promise<void> {
  if (!requireLogin() || questionSubmitting.value) return
  if (!questionText.value.trim()) {
    showToast('请输入问题', 'error')
    return
  }
  questionSubmitting.value = true
  try {
    await addCourseQuestion(courseId.value, questionText.value.trim())
    showToast('提问成功', 'success')
    questionText.value = ''
    await loadQuestions()
  } catch {
    /* 已提示 */
  } finally {
    questionSubmitting.value = false
  }
}

async function onFinishChapter(chapter: CourseChapterVO): Promise<void> {
  if (!requireLogin()) return
  try {
    await finishChapter(chapter.id)
    showToast('已标记学完', 'success')
    await fetchDetail()
  } catch {
    /* 已提示 */
  }
}

function formatDate(value: string): string {
  return value ? value.slice(0, 10) : ''
}

watch(courseId, fetchAll, { immediate: true })
</script>

<template>
  <div class="container detail-page">
    <div v-if="loading" class="detail-skeleton">
      <div class="skeleton skel-crumbs" />
      <div class="skel-head">
        <div class="skeleton skel-cover" />
        <div class="skel-texts">
          <div class="skeleton skel-line wide" />
          <div class="skeleton skel-line" />
          <div class="skeleton skel-line short" />
        </div>
      </div>
      <div class="skeleton skel-tabs" />
    </div>

    <template v-else-if="course">
      <nav class="crumbs" aria-label="面包屑">
        <span>课程中心</span>
        <i class="sep" aria-hidden="true">/</i>
        <RouterLink class="link" :to="`/course/${course.type}`">{{ moduleName }}</RouterLink>
        <i class="sep" aria-hidden="true">/</i>
        <span class="current" :title="course.title">{{ course.title }}</span>
      </nav>

      <section class="detail-head">
        <div class="cover" :style="coverStyle">
          <span class="cover-mark" aria-hidden="true">{{ coverText }}</span>
        </div>

        <div class="head-info">
          <h1 class="title">{{ course.title }}</h1>

          <div class="meta-line">
            <span class="teacher">
              <span class="teacher-avatar" aria-hidden="true">{{ (course.teacher_name || '师').charAt(0) }}</span>
              授课讲师：{{ course.teacher_name || '讲师待定' }}
            </span>
            <span class="tag tag-level-meta">{{ levelLabel }}</span>
          </div>

          <ul class="resource-chips" aria-label="课程资源构成">
            <li v-for="chip in resourceChips" :key="chip.key" class="resource-chip" :class="`rc-${chip.key}`">
              {{ chip.label }}
              <b>{{ chip.count }}</b>
            </li>
            <li v-if="resourceChips.length === 0" class="resource-chip none">资源整理中</li>
          </ul>

          <div class="stat-row">
            <div class="stat">
              <b class="rating-num"><span class="stars" aria-hidden="true">★</span>{{ ratingText }}</b>
              <span>{{ course.rating_count }} 人评价</span>
            </div>
            <div class="stat-divider" aria-hidden="true" />
            <div class="stat">
              <b>{{ course.student_count }}</b>
              <span>人在学</span>
            </div>
          </div>

          <div v-if="course.is_enrolled" class="progress-block">
            <div class="progress-head">
              <span>学习进展</span>
              <span class="progress-percent">{{ course.progress }}%</span>
            </div>
            <div class="progress-track" role="progressbar" :aria-valuenow="course.progress" aria-valuemin="0" aria-valuemax="100">
              <div class="progress-fill" :style="{ transform: `scaleX(${Math.min(100, Math.max(0, course.progress)) / 100})` }" />
            </div>
          </div>

          <div class="actions">
            <span class="price-tag" :class="{ free: course.is_free === 1 }">{{ priceText }}</span>
            <button
              class="btn collect-btn"
              :class="{ on: course.is_collected }"
              type="button"
              :disabled="collectLoading"
              @click="onCollect"
            >
              {{ course.is_collected ? '已收藏' : '收藏' }}
            </button>
            <button
              class="btn btn-primary enroll-btn"
              type="button"
              :disabled="enrollLoading || course.is_enrolled"
              @click="onEnroll"
            >
              {{ course.is_enrolled ? '已报名' : '立即学习' }}
            </button>
          </div>
        </div>
      </section>

      <div class="tab-bar" role="tablist">
        <button
          v-for="tab in (['intro', 'chapters', 'comments', 'questions'] as const)"
          :key="tab"
          type="button"
          role="tab"
          class="tab"
          :class="{ active: activeTab === tab }"
          :aria-selected="activeTab === tab"
          @click="onTabChange(tab)"
        >
          {{ { intro: '课程简介', chapters: '课程目录', comments: '课程评论', questions: '课程答疑' }[tab] }}
          <span v-if="tab === 'chapters'" class="tab-count">{{ chapters.length }}</span>
          <span v-else-if="tab === 'comments'" class="tab-count">{{ commentTotal }}</span>
        </button>
      </div>

      <Transition name="fade" mode="out-in">
        <section v-if="activeTab === 'intro'" key="intro" class="tab-panel">
          <h2 class="panel-title">课程简介</h2>
          <p class="para">{{ course.intro || '本课程正在完善简介，敬请期待。' }}</p>
          <h2 class="panel-title">课程目标</h2>
          <p class="para">{{ course.target || '课程目标整理中，敬请期待。' }}</p>
        </section>

        <section v-else-if="activeTab === 'chapters'" key="chapters" class="tab-panel">
          <div v-if="chapters.length === 0" class="empty-tip">
            <span class="empty-icon" aria-hidden="true">目</span>
            <p>课程目录整理中，敬请期待</p>
          </div>
          <ol v-else class="chapter-list">
            <li v-for="(ch, index) in chapters" :key="ch.id" class="chapter-item">
              <span class="chapter-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <span class="chapter-type" :class="`rtype-${ch.resource_type}`">{{ typeText(ch.resource_type) }}</span>
              <span class="chapter-title" :title="ch.title">{{ ch.title }}</span>
              <span class="chapter-duration">{{ formatDuration(ch.duration) }}</span>
              <button
                v-if="course.is_enrolled"
                class="finish-btn"
                type="button"
                @click="onFinishChapter(ch)"
              >
                学完
              </button>
            </li>
          </ol>
        </section>

        <section v-else-if="activeTab === 'comments'" key="comments" class="tab-panel">
          <form v-if="course.is_enrolled" class="comment-form" @submit.prevent="submitComment">
            <div class="rate-row">
              <span class="rate-label">评分</span>
              <div class="stars-select" role="radiogroup" aria-label="评分">
                <button
                  v-for="n in 5"
                  :key="n"
                  type="button"
                  class="star-btn"
                  :class="{ active: n <= commentForm.score }"
                  :aria-label="`${n} 星`"
                  @click="commentForm.score = n"
                >
                  ★
                </button>
              </div>
            </div>
            <textarea
              v-model="commentForm.content"
              class="form-input textarea"
              placeholder="分享你的学习体验…"
              maxlength="500"
            />
            <div class="comment-actions">
              <button class="btn btn-primary btn-sm" type="submit" :disabled="commentSubmitting">
                {{ commentSubmitting ? '提交中…' : '发表评价' }}
              </button>
            </div>
          </form>
          <p v-else class="comment-hint">报名课程后可参与评价</p>

          <div v-if="comments.length === 0" class="empty-tip">
            <span class="empty-icon" aria-hidden="true">评</span>
            <p>暂无评论，快来抢沙发</p>
          </div>
          <ul v-else class="comment-list">
            <li v-for="c in comments" :key="c.id" class="comment-item">
              <span class="comment-avatar" aria-hidden="true">{{ (c.nickname || '学').charAt(0).toUpperCase() }}</span>
              <div class="comment-body">
                <div class="comment-head">
                  <span class="comment-name">{{ c.nickname || '学员' }}</span>
                  <span class="comment-stars">{{ '★'.repeat(c.score) }}</span>
                  <span class="comment-date">{{ formatDate(c.created_at) }}</span>
                </div>
                <p class="comment-content">{{ c.content }}</p>
              </div>
            </li>
          </ul>
          <div v-if="comments.length < commentTotal" class="load-more">
            <button class="btn" type="button" :disabled="commentsLoading" @click="loadMoreComments">
              {{ commentsLoading ? '加载中…' : '加载更多' }}
            </button>
          </div>
        </section>

        <section v-else key="questions" class="tab-panel">
          <form class="question-form" @submit.prevent="submitQuestion">
            <textarea
              v-model="questionText"
              class="form-input textarea"
              placeholder="向讲师提问…"
              maxlength="500"
            />
            <div class="comment-actions">
              <button class="btn btn-primary btn-sm" type="submit" :disabled="questionSubmitting">
                {{ questionSubmitting ? '提交中…' : '提问' }}
              </button>
            </div>
          </form>

          <div v-if="questions.length === 0" class="empty-tip">
            <span class="empty-icon" aria-hidden="true">答</span>
            <p>暂无答疑，有疑问欢迎提问</p>
          </div>
          <ul v-else class="question-list">
            <li v-for="q in questions" :key="q.id" class="question-item">
              <div class="q-row">
                <span class="q-badge">问</span>
                <div class="q-main">
                  <p class="q-text">{{ q.question }}</p>
                  <p class="q-meta">{{ q.nickname || '学员' }} · {{ formatDate(q.created_at) }}</p>
                </div>
              </div>
              <div v-if="q.status === 1" class="a-row">
                <span class="a-badge">答</span>
                <p class="a-text">{{ q.answer }}</p>
              </div>
              <p v-else class="a-pending">待回复</p>
            </li>
          </ul>
        </section>
      </Transition>
    </template>

    <!-- 支付弹层：收费课程下单后唤起 -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="payDialog.visible" class="pay-mask" @click.self="closePayDialog">
          <div class="pay-dialog" role="dialog" aria-modal="true" aria-label="课程支付">
            <h2 class="pay-title">确认支付</h2>
            <p class="pay-amount">¥{{ payDialog.amount.toFixed(2) }}</p>
            <p class="pay-order">订单号 {{ payDialog.orderNo }}</p>

            <p class="pay-label">选择支付方式</p>
            <div class="pay-types" role="radiogroup" aria-label="支付方式">
              <button
                v-for="opt in PAY_TYPE_OPTIONS"
                :key="opt.value"
                type="button"
                class="pay-type"
                :class="{ active: payDialog.payType === opt.value }"
                @click="payDialog.payType = opt.value"
              >
                {{ opt.label }}
              </button>
            </div>

            <div class="pay-actions">
              <button class="btn" type="button" :disabled="payDialog.loading" @click="closePayDialog">取消</button>
              <button class="btn btn-primary" type="button" :disabled="payDialog.loading" @click="confirmPay">
                {{ payDialog.loading ? '支付中…' : '确认支付' }}
              </button>
            </div>
            <p class="pay-hint">点击确认支付即完成支付</p>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.detail-page {
  min-width: 0;
}

/* ---------- 骨架 ---------- */
.detail-skeleton { padding-top: var(--space-2); }
.skel-crumbs { width: 260px; height: 18px; margin-bottom: var(--space-4); border-radius: 4px; }
.skel-head { display: flex; gap: var(--space-5); padding: var(--space-5); background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.skel-cover { width: 280px; height: 190px; border-radius: var(--radius-md); }
.skel-texts { flex: 1; display: flex; flex-direction: column; gap: var(--space-4); justify-content: center; }
.skel-line { height: 18px; width: 60%; border-radius: 4px; }
.skel-line.wide { width: 85%; height: 24px; }
.skel-line.short { width: 36%; }
.skel-tabs { height: 46px; margin-top: var(--space-4); border-radius: var(--radius-md); }

/* ---------- 面包屑 ---------- */
.crumbs { display: flex; align-items: center; flex-wrap: wrap; gap: var(--space-2); margin-bottom: var(--space-4); font-size: 13px; color: var(--color-text-tertiary); }
.crumbs .sep { color: var(--color-border-strong); }
.crumbs .link { color: var(--color-primary); }
.crumbs .link:hover { text-decoration: underline; }
.crumbs .current { max-width: 520px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: var(--color-text); font-weight: 500; }

/* ---------- 详情头 ---------- */
.detail-head { display: flex; gap: var(--space-6); padding: var(--space-5); background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); box-shadow: var(--shadow-sm); }
.cover { position: relative; display: flex; align-items: center; justify-content: center; width: 280px; height: 190px; flex-shrink: 0; overflow: hidden; border-radius: var(--radius-md); color: #fff; }
.cover-mark { font-size: 64px; font-weight: 700; line-height: 1; opacity: 0.88; user-select: none; }
.head-info { flex: 1; min-width: 0; display: flex; flex-direction: column; }
.title { font-size: 22px; font-weight: 700; line-height: 1.35; letter-spacing: -0.01em; }
.meta-line { display: flex; align-items: center; gap: var(--space-3); margin-top: var(--space-3); }
.teacher { display: inline-flex; align-items: center; gap: 8px; font-size: 14px; color: var(--color-text-secondary); }
.teacher-avatar { display: inline-flex; align-items: center; justify-content: center; width: 22px; height: 22px; border-radius: 50%; background: var(--color-primary-soft); color: var(--color-primary); font-size: 12px; font-weight: 600; }
.tag { padding: 2px 10px; border-radius: 999px; font-size: 12px; }
.tag-level-meta { background: var(--color-bg); color: var(--color-text-secondary); }
.resource-chips { display: flex; flex-wrap: wrap; gap: var(--space-2); margin-top: var(--space-3); }
.resource-chip { display: inline-flex; align-items: center; gap: 6px; padding: 4px 12px; border-radius: 999px; font-size: 12px; color: var(--color-text-secondary); background: var(--color-bg); }
.resource-chip b { font-family: var(--font-num); font-size: 13px; }
.rc-slide b { color: var(--color-primary); }
.rc-video b { color: var(--color-success); }
.rc-lab b { color: var(--color-warning); }
.resource-chip.none { color: var(--color-text-tertiary); }
.stat-row { display: flex; align-items: center; gap: var(--space-5); margin-top: var(--space-5); }
.stat { display: flex; flex-direction: column; }
.stat b { font-size: 20px; font-weight: 700; font-family: var(--font-num); line-height: 1.2; }
.stat .rating-num { display: inline-flex; align-items: center; gap: 4px; }
.stat .rating-num .stars { font-size: 14px; }
.stat span { margin-top: 2px; font-size: 12px; color: var(--color-text-tertiary); }
.stat-divider { width: 1px; height: 34px; background: var(--color-border); }
.progress-block { margin-top: var(--space-4); max-width: 420px; }
.progress-head { display: flex; justify-content: space-between; margin-bottom: 6px; font-size: 12px; color: var(--color-text-secondary); }
.progress-percent { color: var(--color-primary); font-weight: 600; font-family: var(--font-num); }
.progress-track { height: 6px; overflow: hidden; border-radius: 999px; background: var(--color-bg); }
.progress-fill { height: 100%; border-radius: 999px; background: var(--color-primary); transition: transform var(--dur-base) var(--ease-out); }
.actions { display: flex; align-items: center; gap: var(--space-2); margin-top: auto; padding-top: var(--space-4); }
.price-tag { margin-right: auto; font-family: var(--font-num); font-size: 20px; font-weight: 700; letter-spacing: -0.01em; color: var(--color-primary); }
.price-tag.free { color: var(--color-success); }
.collect-btn.on { color: var(--color-primary); border-color: var(--color-primary-border); background: var(--color-primary-soft); }
.enroll-btn:disabled { opacity: 0.6; }

/* ---------- Tab ---------- */
.tab-bar { display: flex; gap: var(--space-2); margin-top: var(--space-4); padding: 4px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.tab { display: inline-flex; align-items: center; gap: 6px; padding: 9px 24px; border-radius: var(--radius-md); font-size: 15px; color: var(--color-text-secondary); transition: all var(--dur-fast) ease; }
.tab:hover { color: var(--color-primary); }
.tab.active { background: var(--color-primary); color: #fff; font-weight: 600; }
.tab-count { min-width: 18px; height: 18px; padding: 0 5px; border-radius: 999px; background: rgba(255, 255, 255, 0.24); font-size: 11px; line-height: 18px; font-family: var(--font-num); }
.tab:not(.active) .tab-count { background: var(--color-bg); color: var(--color-text-tertiary); }
.tab-panel { margin-top: var(--space-3); padding: var(--space-6); background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-md); }
.panel-title { margin: var(--space-2) 0 var(--space-3); font-size: 16px; font-weight: 600; }
.para { margin-bottom: var(--space-6); color: var(--color-text-secondary); line-height: 1.85; white-space: pre-wrap; }
.para:last-child { margin-bottom: 0; }

/* ---------- 目录 ---------- */
.chapter-list { display: flex; flex-direction: column; }
.chapter-item { display: flex; align-items: center; gap: var(--space-4); padding: 13px var(--space-2); border-bottom: 1px dashed var(--color-border); font-size: 14px; transition: background var(--dur-fast) ease; }
.chapter-item:hover { background: var(--color-bg); }
.chapter-item:last-child { border-bottom: none; }
.chapter-index { width: 30px; color: var(--color-text-tertiary); font-family: var(--font-num); font-size: 13px; }
.chapter-type { flex-shrink: 0; min-width: 44px; padding: 2px 9px; border-radius: 999px; font-size: 12px; text-align: center; }
.rtype-0 { background: var(--color-primary-soft); color: var(--color-primary); }
.rtype-1 { background: var(--color-success-soft); color: var(--color-success); }
.rtype-2 { background: var(--color-warning-soft); color: var(--color-warning); }
.chapter-title { flex: 1; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.chapter-duration { color: var(--color-text-tertiary); font-size: 13px; font-family: var(--font-num); }
.finish-btn { flex-shrink: 0; padding: 3px 10px; border: 1px solid var(--color-border-strong); border-radius: 999px; font-size: 12px; color: var(--color-text-secondary); transition: all var(--dur-fast) ease; }
.finish-btn:hover { border-color: var(--color-primary-border); color: var(--color-primary); }

/* ---------- 评论 ---------- */
.comment-form, .question-form { margin-bottom: var(--space-5); }
.rate-row { display: flex; align-items: center; gap: var(--space-3); margin-bottom: var(--space-3); }
.rate-label { font-size: 13px; font-weight: 600; }
.stars-select { display: flex; gap: 2px; }
.star-btn { font-size: 22px; color: var(--color-border-strong); transition: color var(--dur-fast) ease, transform var(--dur-fast) ease; }
.star-btn.active { color: #e0a800; }
.star-btn:hover { transform: scale(1.1); }
.textarea { height: auto; min-height: 88px; padding: 12px 14px; resize: vertical; }
.comment-actions { display: flex; justify-content: flex-end; margin-top: var(--space-3); }
.comment-hint { padding: var(--space-4); margin-bottom: var(--space-4); text-align: center; font-size: 13px; color: var(--color-text-tertiary); background: var(--color-bg); border-radius: var(--radius-md); }
.comment-list { display: flex; flex-direction: column; }
.comment-item { display: flex; gap: var(--space-3); padding: var(--space-4) 0; border-bottom: 1px dashed var(--color-border); }
.comment-item:last-child { border-bottom: none; }
.comment-avatar { display: inline-flex; align-items: center; justify-content: center; width: 40px; height: 40px; flex-shrink: 0; border-radius: 50%; background: var(--color-primary-soft); color: var(--color-primary); font-weight: 700; }
.comment-body { flex: 1; min-width: 0; }
.comment-head { display: flex; align-items: center; gap: var(--space-2); }
.comment-name { font-size: 14px; font-weight: 600; }
.comment-stars { color: #e0a800; font-size: 13px; }
.comment-date { margin-left: auto; font-size: 12px; color: var(--color-text-tertiary); }
.comment-content { margin-top: 4px; font-size: 14px; color: var(--color-text-secondary); line-height: 1.7; white-space: pre-wrap; }
.load-more { display: flex; justify-content: center; margin-top: var(--space-4); }

/* ---------- 答疑 ---------- */
.question-list { display: flex; flex-direction: column; }
.question-item { padding: var(--space-4) 0; border-bottom: 1px dashed var(--color-border); }
.question-item:last-child { border-bottom: none; }
.q-row, .a-row { display: flex; gap: var(--space-3); }
.q-badge, .a-badge { display: inline-flex; align-items: center; justify-content: center; width: 24px; height: 24px; flex-shrink: 0; border-radius: 6px; font-size: 12px; font-weight: 700; }
.q-badge { background: var(--color-primary-soft); color: var(--color-primary); }
.a-badge { background: var(--color-success-soft); color: var(--color-success); }
.q-main { flex: 1; min-width: 0; }
.q-text { font-size: 14px; line-height: 1.7; }
.q-meta { margin-top: 2px; font-size: 12px; color: var(--color-text-tertiary); }
.a-row { margin-top: var(--space-3); padding: var(--space-3); background: var(--color-bg); border-radius: var(--radius-md); }
.a-text { flex: 1; font-size: 14px; color: var(--color-text-secondary); line-height: 1.7; }
.a-pending { margin-top: var(--space-3); font-size: 12px; color: var(--color-warning); }

/* ---------- 支付弹层 ---------- */
.pay-mask { position: fixed; inset: 0; z-index: 9000; display: flex; align-items: center; justify-content: center; padding: var(--space-4); background: rgba(18, 28, 45, 0.45); }
.pay-dialog { width: min(400px, 100%); padding: var(--space-6); background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-lg); }
.pay-title { font-size: 16px; font-weight: 600; }
.pay-amount { margin-top: var(--space-4); font-family: var(--font-num); font-size: 32px; font-weight: 700; letter-spacing: -0.02em; color: var(--color-primary); }
.pay-order { margin-top: 4px; font-family: var(--font-num); font-size: 12px; color: var(--color-text-tertiary); }
.pay-label { margin: var(--space-5) 0 var(--space-2); font-size: 13px; font-weight: 600; }
.pay-types { display: flex; gap: var(--space-2); }
.pay-type { flex: 1; padding: 12px 0; border: 1px solid var(--color-border-strong); border-radius: var(--radius-md); font-size: 14px; color: var(--color-text-secondary); transition: all var(--dur-fast) ease; }
.pay-type:hover { border-color: var(--color-primary-border); color: var(--color-primary); }
.pay-type.active { border-color: var(--color-primary); background: var(--color-primary-soft); color: var(--color-primary); font-weight: 600; }
.pay-actions { display: flex; justify-content: flex-end; gap: var(--space-2); margin-top: var(--space-5); }
.pay-hint { margin-top: var(--space-3); font-size: 12px; text-align: center; color: var(--color-text-tertiary); }

@media (max-width: 760px) {
  .detail-head { flex-direction: column; }
  .cover { width: 100%; height: auto; aspect-ratio: 16 / 9; }
  .stat-row { flex-wrap: wrap; }
}
</style>
