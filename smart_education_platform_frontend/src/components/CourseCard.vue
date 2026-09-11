<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import {
  COURSE_LEVEL_MAP,
  RESOURCE_TYPE_MAP,
  toNumber,
  type CourseCardVO,
} from '@/types/api'

const props = defineProps<{ course: CourseCardVO }>()

const router = useRouter()

/** 按课程类型取色相：同屏卡片色彩成体系，而非随机配色 */
const HUE_BY_TYPE: Record<number, number> = { 0: 212, 1: 176, 2: 28 }

const hue = computed(() => HUE_BY_TYPE[props.course.type] ?? 212)

/** 封面为同色系的极浅渐层，文字取该色系的深调，整体安静克制 */
const coverStyle = computed(() => ({
  background: `linear-gradient(150deg, hsl(${hue.value} 34% 97%) 0%, hsl(${hue.value} 28% 92%) 100%)`,
  color: `hsl(${hue.value} 36% 38%)`,
}))

const coverText = computed(() => props.course.title.trim().charAt(0))

const levelLabel = computed(
  () => COURSE_LEVEL_MAP.get(props.course.level) || `Lv.${props.course.level}`,
)

/** 讲师 + 资源构成合并为一行次要信息，避免卡片信息分层过碎 */
const metaText = computed(() => {
  const parts: string[] = []
  if (props.course.teacher_name) parts.push(props.course.teacher_name)
  const resources: string[] = []
  if (props.course.courseware_count > 0) {
    resources.push(`${RESOURCE_TYPE_MAP.get(0)} ${props.course.courseware_count}`)
  }
  if (props.course.video_count > 0) {
    resources.push(`${RESOURCE_TYPE_MAP.get(1)} ${props.course.video_count}`)
  }
  if (props.course.lab_count > 0) {
    resources.push(`${RESOURCE_TYPE_MAP.get(2)} ${props.course.lab_count}`)
  }
  if (resources.length) parts.push(resources.join(' / '))
  return parts.join(' · ') || '课程资源整理中'
})

const score = computed(() => toNumber(props.course.score))
const hasScore = computed(() => score.value > 0)

const studentText = computed(() => {
  const n = props.course.student_count
  return n >= 10000 ? `${(n / 10000).toFixed(1)}w` : String(n)
})

const isFree = computed(() => props.course.is_free === 1)
const priceText = computed(() =>
  isFree.value ? '免费' : `¥${toNumber(props.course.price).toFixed(2)}`,
)

function goDetail(): void {
  router.push(`/course/detail/${props.course.id}`)
}
</script>

<template>
  <article
    class="course-card lift"
    role="button"
    tabindex="0"
    :aria-label="`查看课程：${course.title}`"
    @click="goDetail"
    @keydown.enter="goDetail"
  >
    <div class="card-cover" :style="coverStyle">
      <span class="cover-watermark" aria-hidden="true">{{ coverText }}</span>
      <span class="cover-badge">{{ levelLabel }}</span>
    </div>

    <div class="card-body">
      <h3 class="card-title" :title="course.title">{{ course.title }}</h3>
      <p class="card-meta" :title="metaText">{{ metaText }}</p>

      <div class="card-foot">
        <span v-if="hasScore" class="rating">
          <span class="star" aria-hidden="true">★</span>
          <b>{{ score.toFixed(1) }}</b>
        </span>
        <span v-else class="muted">暂无评分</span>
        <span class="muted">{{ studentText }} 人在学</span>
        <span class="price" :class="{ free: isFree }">{{ priceText }}</span>
      </div>
    </div>
  </article>
</template>

<style scoped>
.course-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
}

.course-card:hover {
  border-color: var(--color-border-strong);
}

/* 封面：极浅同色系渐层 + 水印首字，承担色彩节奏而非装饰堆叠 */
.card-cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  overflow: hidden;
}

.cover-watermark {
  font-size: 56px;
  font-weight: 700;
  line-height: 1;
  opacity: 0.15;
  user-select: none;
}

.cover-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 2px 10px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
  font-size: 11px;
  font-weight: 600;
  letter-spacing: var(--tracking-wide);
}

.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 14px 16px 15px;
}

.card-title {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  min-height: 43px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
  letter-spacing: var(--tracking-tight);
}

.card-meta {
  margin-top: 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12.5px;
  color: var(--color-text-tertiary);
}

.card-foot {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin-top: auto;
  padding-top: 12px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.rating {
  display: inline-flex;
  align-items: baseline;
  gap: 3px;
}

.rating .star {
  color: #d99a00;
  font-size: 11px;
}

.rating b {
  font-family: var(--font-num);
  font-size: 12.5px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.muted {
  color: var(--color-text-tertiary);
}

.price {
  margin-left: auto;
  font-family: var(--font-num);
  font-size: 13px;
  font-weight: 600;
  color: var(--color-primary);
}

.price.free {
  color: var(--color-success);
}
</style>
