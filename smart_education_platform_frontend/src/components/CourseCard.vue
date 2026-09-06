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

const coverText = computed(() => props.course.title.trim().charAt(0).toUpperCase() || '课')

const resources = computed(() => {
  const list: string[] = []
  if (props.course.courseware_count > 0) {
    list.push(`${RESOURCE_TYPE_MAP.get(0)} ${props.course.courseware_count}`)
  }
  if (props.course.video_count > 0) {
    list.push(`${RESOURCE_TYPE_MAP.get(1)} ${props.course.video_count}`)
  }
  if (props.course.lab_count > 0) {
    list.push(`${RESOURCE_TYPE_MAP.get(2)} ${props.course.lab_count}`)
  }
  return list
})

const resourcesText = computed(() => resources.value.join(' · ') || '资源整理中')

const levelLabel = computed(
  () => COURSE_LEVEL_MAP.get(props.course.level) || `Lv.${props.course.level}`,
)

const priceLabel = computed(() =>
  props.course.is_free === 1 ? '免费' : `¥${toNumber(props.course.price).toFixed(2)}`,
)

const ratingText = computed(() => {
  const n = toNumber(props.course.score)
  return n > 0 ? n.toFixed(1) : '暂无评分'
})

const studentText = computed(() => {
  const n = props.course.student_count
  return n >= 10000 ? `${(n / 10000).toFixed(1)}w` : String(n)
})

function goDetail(): void {
  router.push(`/course/detail/${props.course.id}`)
}
</script>

<template>
  <article class="course-card lift" role="button" tabindex="0" :aria-label="`查看课程：${course.title}`" @click="goDetail" @keydown.enter="goDetail">
    <div class="card-cover" :style="{ background: `hsl(${(course.id * 47) % 360} 45% 40%)` }">
      <span class="cover-text" aria-hidden="true">{{ coverText }}</span>
      <div class="cover-shade" aria-hidden="true" />
      <span class="tag tag-level">{{ levelLabel }}</span>
      <span class="tag" :class="course.is_free === 1 ? 'tag-free' : 'tag-fee'">{{ priceLabel }}</span>
    </div>

    <div class="card-body">
      <h3 class="card-title" :title="course.title">{{ course.title }}</h3>
      <p class="card-teacher">
        <span class="teacher-dot" aria-hidden="true" />
        {{ course.teacher_name || '讲师待定' }}
      </p>
      <p class="card-resources">{{ resourcesText }}</p>
      <div class="card-footer">
        <span class="rating">
          <span class="stars" aria-hidden="true">★</span>
          <b>{{ ratingText }}</b>
        </span>
        <span class="students">{{ studentText }} 人在学</span>
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
  box-shadow: var(--shadow-sm);
}

.card-cover {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  color: rgba(255, 255, 255, 0.96);
}

.cover-text {
  position: relative;
  z-index: 1;
  font-size: 46px;
  font-weight: 700;
  letter-spacing: 0.02em;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.16);
}

/* 顶部收拢暗角，增强层次而非装饰 */
.cover-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(20, 30, 55, 0.14) 0%, transparent 32%);
}

.card-cover .tag {
  position: absolute;
  top: 10px;
  z-index: 2;
  border-radius: 999px;
  backdrop-filter: blur(4px);
}

.tag-level {
  left: 10px;
  background: rgba(23, 35, 61, 0.35);
  color: #fff;
}

.tag-free,
.tag-fee {
  right: 10px;
}

.card-body {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding: 14px 16px 16px;
}

.card-title {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  min-height: 42px;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.35;
}

.card-teacher {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.teacher-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-primary);
}

.card-resources {
  margin-top: 2px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: auto;
  padding-top: 12px;
  font-size: 13px;
}

.rating {
  display: inline-flex;
  align-items: baseline;
  gap: 4px;
}

.rating b {
  font-size: 15px;
  font-family: var(--font-num);
}

.rating .stars {
  font-size: 12px;
}

.students {
  color: var(--color-text-tertiary);
  font-size: 12px;
}
</style>
