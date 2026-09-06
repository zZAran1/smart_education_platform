<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCourseChapters, getCourseDetail } from '@/api/course'
import {
  COURSE_LEVEL_MAP,
  COURSE_TYPE_MAP,
  RESOURCE_TYPE_MAP,
  formatDuration,
  toNumber,
  type CourseChapterVO,
  type CourseDetailVO,
} from '@/types/api'

function typeText(type: number): string {
  return RESOURCE_TYPE_MAP.get(type) || '资源'
}

const route = useRoute()
const router = useRouter()

const course = ref<CourseDetailVO | null>(null)
const chapters = ref<CourseChapterVO[]>([])
const loading = ref(true)
const activeTab = ref<'intro' | 'chapters'>('intro')

const courseId = computed(() => Number(route.params.id))

const moduleName = computed(() => COURSE_TYPE_MAP.get(course.value?.type ?? 0) || '课程中心')

const coverText = computed(() => (course.value?.title || '课').trim().charAt(0).toUpperCase())

interface ResourceChip {
  label: string
  count: number
  key: string
}

/** 资源统计：课件 / 视频 / 实验 */
const resourceChips = computed<ResourceChip[]>(() => {
  const c = course.value
  if (!c) return []
  const list: ResourceChip[] = []
  if (c.courseware_count > 0) list.push({ label: '课件', count: c.courseware_count, key: 'slide' })
  if (c.video_count > 0) list.push({ label: '视频', count: c.video_count, key: 'video' })
  if (c.lab_count > 0) list.push({ label: '实验', count: c.lab_count, key: 'lab' })
  return list
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

async function fetchAll(): Promise<void> {
  loading.value = true
  activeTab.value = 'intro'
  try {
    const [detail, list] = await Promise.all([
      getCourseDetail(courseId.value),
      getCourseChapters(courseId.value),
    ])
    course.value = detail
    chapters.value = list
  } catch {
    // 课程不存在/网络异常（已提示）：返回课程中心
    router.replace(`/course/${course.value?.type ?? 0}`)
  } finally {
    loading.value = false
  }
}

function backToList(): void {
  router.push(`/course/${course.value?.type ?? 0}`)
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
        <button class="link" type="button" @click="backToList">{{ moduleName }}</button>
        <i class="sep" aria-hidden="true">/</i>
        <span class="current" :title="course.title">{{ course.title }}</span>
      </nav>

      <section class="detail-head">
        <div class="cover" :style="{ background: `hsl(${(course.id * 47) % 360} 50% 50%)` }">
          <span class="cover-text" aria-hidden="true">{{ coverText }}</span>
          <span class="cover-level">{{ levelLabel }}</span>
          <span class="cover-price">{{ priceText }}</span>
        </div>

        <div class="head-info">
          <h1 class="title">{{ course.title }}</h1>

          <div class="meta-line">
            <span class="teacher">
              <span class="teacher-avatar" aria-hidden="true">
                {{ (course.teacher_name || '师').charAt(0) }}
              </span>
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
              <div
                class="progress-fill"
                :style="{ transform: `scaleX(${Math.min(100, Math.max(0, course.progress)) / 100})` }"
              />
            </div>
          </div>

          <div class="flags">
            <span class="flag" :class="course.is_enrolled ? 'on' : 'off'">
              <i aria-hidden="true" />{{ course.is_enrolled ? '已报名' : '尚未报名' }}
            </span>
            <span class="flag" :class="course.is_collected ? 'on' : 'off'">
              <i aria-hidden="true" />{{ course.is_collected ? '已收藏' : '未收藏' }}
            </span>
          </div>
        </div>
      </section>

      <div class="tab-bar" role="tablist">
        <button
          type="button"
          role="tab"
          class="tab"
          :class="{ active: activeTab === 'intro' }"
          :aria-selected="activeTab === 'intro'"
          @click="activeTab = 'intro'"
        >
          课程简介
        </button>
        <button
          type="button"
          role="tab"
          class="tab"
          :class="{ active: activeTab === 'chapters' }"
          :aria-selected="activeTab === 'chapters'"
          @click="activeTab = 'chapters'"
        >
          课程目录
          <span class="tab-count">{{ chapters.length }}</span>
        </button>
      </div>

      <Transition name="fade" mode="out-in">
        <section v-if="activeTab === 'intro'" key="intro" class="tab-panel">
          <h2 class="panel-title">课程简介</h2>
          <p class="para">{{ course.intro || '本课程正在完善简介，敬请期待。' }}</p>
          <h2 class="panel-title">课程目标</h2>
          <p class="para">{{ course.target || '课程目标整理中，敬请期待。' }}</p>
        </section>

        <section v-else key="chapters" class="tab-panel">
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
            </li>
          </ol>
        </section>
      </Transition>
    </template>
  </div>
</template>

<style scoped>
.detail-page {
  min-width: 0;
}

/* ---------- 骨架 ---------- */
.detail-skeleton {
  padding-top: var(--space-2);
}

.skel-crumbs {
  width: 260px;
  height: 18px;
  margin-bottom: var(--space-4);
  border-radius: 4px;
}

.skel-head {
  display: flex;
  gap: var(--space-5);
  padding: var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.skel-cover {
  width: 280px;
  height: 190px;
  border-radius: var(--radius-md);
}

.skel-texts {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
  justify-content: center;
}

.skel-line {
  height: 18px;
  width: 60%;
  border-radius: 4px;
}

.skel-line.wide {
  width: 85%;
  height: 24px;
}

.skel-line.short {
  width: 36%;
}

.skel-tabs {
  height: 46px;
  margin-top: var(--space-4);
  border-radius: var(--radius-md);
}

/* ---------- 面包屑 ---------- */
.crumbs {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-4);
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.crumbs .sep {
  color: var(--color-border-strong);
}

.crumbs .link {
  color: var(--color-primary);
}

.crumbs .link:hover {
  text-decoration: underline;
}

.crumbs .current {
  max-width: 520px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 500;
}

/* ---------- 详情头 ---------- */
.detail-head {
  display: flex;
  gap: var(--space-6);
  padding: var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 280px;
  height: 190px;
  flex-shrink: 0;
  overflow: hidden;
  border-radius: var(--radius-md);
  color: rgba(255, 255, 255, 0.96);
}

.cover-text {
  font-size: 68px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-shadow: 0 2px 16px rgba(0, 0, 0, 0.2);
}

.cover-level {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  background: rgba(23, 35, 61, 0.4);
  color: #fff;
  font-size: 12px;
}

.cover-price {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.22);
  color: #fff;
}

.head-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.title {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: -0.01em;
}

.meta-line {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-3);
}

.teacher {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.teacher-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 12px;
  font-weight: 600;
}

.resource-chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-3);
}

.resource-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--color-text-secondary);
  background: var(--color-bg);
}

.resource-chip b {
  font-family: var(--font-num);
  font-size: 13px;
}

.rc-slide b {
  color: var(--color-primary);
}

.rc-video b {
  color: var(--color-success);
}

.rc-lab b {
  color: var(--color-warning);
}

.resource-chip.none {
  font-style: normal;
  color: var(--color-text-tertiary);
}

.stat-row {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-top: var(--space-5);
}

.stat {
  display: flex;
  flex-direction: column;
}

.stat b {
  font-size: 20px;
  font-weight: 700;
  font-family: var(--font-num);
  line-height: 1.2;
}

.stat .rating-num {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.stat .rating-num .stars {
  font-size: 14px;
}

.stat span {
  margin-top: 2px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.stat-divider {
  width: 1px;
  height: 34px;
  background: var(--color-border);
}

.progress-block {
  margin-top: var(--space-4);
  max-width: 420px;
}

.progress-head {
  display: flex;
  justify-content: space-between;
  margin-bottom: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.progress-percent {
  color: var(--color-primary);
  font-weight: 600;
  font-family: var(--font-num);
}

.progress-track {
  height: 6px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--color-bg);
}

.progress-fill {
  height: 100%;
  border-radius: 999px;
  background: var(--color-primary);
  transition: transform var(--dur-base) var(--ease-out);
}

.flags {
  display: flex;
  gap: var(--space-2);
  margin-top: auto;
  padding-top: var(--space-4);
}

.flag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.flag i {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border-strong);
}

.flag.on {
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.flag.on i {
  background: var(--color-primary);
}

.flag.off {
  background: var(--color-bg);
}

/* ---------- Tab ---------- */
.tab-bar {
  display: flex;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding: 4px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 9px 24px;
  border-radius: var(--radius-md);
  font-size: 15px;
  color: var(--color-text-secondary);
  transition: all var(--dur-fast) ease;
}

.tab:hover {
  color: var(--color-primary);
}

.tab.active {
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.24);
  font-size: 11px;
  line-height: 18px;
  font-family: var(--font-num);
}

.tab:not(.active) .tab-count {
  background: var(--color-bg);
  color: var(--color-text-tertiary);
}

.tab-panel {
  margin-top: var(--space-3);
  padding: var(--space-6);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.panel-title {
  margin: var(--space-2) 0 var(--space-3);
  font-size: 16px;
  font-weight: 600;
}

.para {
  margin-bottom: var(--space-6);
  color: var(--color-text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
}

.para:last-child {
  margin-bottom: 0;
}

/* ---------- 目录 ---------- */
.chapter-list {
  display: flex;
  flex-direction: column;
}

.chapter-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: 13px var(--space-2);
  border-bottom: 1px dashed var(--color-border);
  font-size: 14px;
  transition: background var(--dur-fast) ease;
}

.chapter-item:hover {
  background: var(--color-bg);
}

.chapter-item:last-child {
  border-bottom: none;
}

.chapter-index {
  width: 30px;
  color: var(--color-text-tertiary);
  font-family: var(--font-num);
  font-size: 13px;
}

.chapter-type {
  flex-shrink: 0;
  min-width: 44px;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 12px;
  text-align: center;
}

.rtype-0 {
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.rtype-1 {
  background: var(--color-success-soft);
  color: var(--color-success);
}

.rtype-2 {
  background: var(--color-warning-soft);
  color: var(--color-warning);
}

.chapter-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chapter-duration {
  color: var(--color-text-tertiary);
  font-size: 13px;
  font-family: var(--font-num);
}

@media (max-width: 760px) {
  .detail-head {
    flex-direction: column;
  }

  .cover {
    width: 100%;
    height: auto;
    aspect-ratio: 16 / 9;
  }

  .stat-row {
    flex-wrap: wrap;
  }
}
</style>
