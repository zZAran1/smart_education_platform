<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getCourseCategories, getCoursePage } from '@/api/course'
import CourseCard from '@/components/CourseCard.vue'
import CategoryFilter from '@/components/CategoryFilter.vue'
import Pagination from '@/components/Pagination.vue'
import {
  COURSE_LEVEL_OPTIONS,
  COURSE_SORT_OPTIONS,
  COURSE_TYPE_MAP,
  type CourseCardVO,
  type CourseCategoryVO,
} from '@/types/api'

const route = useRoute()

const PAGE_SIZE = 12
const SKELETON_COUNT = 8

const type = computed(() => {
  const raw = route.params.type as string
  const t = Number.parseInt(raw, 10)
  return Number.isNaN(t) ? 0 : t
})

const moduleName = computed(() => COURSE_TYPE_MAP.get(type.value) || '课程中心')

const categories = ref<CourseCategoryVO[]>([])
const courses = ref<CourseCardVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)

const filters = reactive<{
  tech_system_id: number | null
  tech_direction_id: number | null
  level: number | null
  is_free: number | null
  keyword: string
  sort_by: string
}>({
  tech_system_id: null,
  tech_direction_id: null,
  level: null,
  is_free: null,
  keyword: '',
  sort_by: 'publish_time',
})

const categoryFilter = computed(() => ({
  tech_system_id: filters.tech_system_id,
  tech_direction_id: filters.tech_direction_id,
}))

const hasKeyword = computed(() => filters.keyword.trim().length > 0)

const sortLabel = computed(
  () => COURSE_SORT_OPTIONS.find((s) => s.value === filters.sort_by)?.label || '',
)

/** 下拉选择空值统一转 null 展示"全部" */
const levelSelect = computed({
  get: () => (filters.level === null ? '' : String(filters.level)),
  set: (v: string) => (filters.level = v === '' ? null : Number(v)),
})

const freeSelect = computed({
  get: () => (filters.is_free === null ? '' : String(filters.is_free)),
  set: (v: string) => (filters.is_free = v === '' ? null : Number(v)),
})

/** 分类切换：同步到筛选，重置分页 */
function onCategoryChange(value: { tech_system_id?: number | null; tech_direction_id?: number | null }): void {
  filters.tech_system_id = value.tech_system_id ?? null
  filters.tech_direction_id = value.tech_direction_id ?? null
  resetAndFetch()
}

/** 条件变更（等级/免费/搜索/排序） */
function onFilterChange(): void {
  resetAndFetch()
}

function doSearch(): void {
  resetAndFetch()
}

function resetAndFetch(): void {
  pageNum.value = 1
  fetchCourses()
}

function changePage(p: number): void {
  pageNum.value = p
  fetchCourses()
}

/** 加载课程列表 */
async function fetchCourses(): Promise<void> {
  loading.value = true
  try {
    const data = await getCoursePage({
      type: type.value,
      tech_system_id: filters.tech_system_id,
      tech_direction_id: filters.tech_direction_id,
      level: filters.level,
      is_free: filters.is_free,
      keyword: filters.keyword.trim(),
      sort_by: filters.sort_by,
      page_num: pageNum.value,
      page_size: PAGE_SIZE,
    })
    courses.value = data.records || []
    total.value = data.total || 0
  } catch {
    /* 错误提示已由 request 拦截层处理 */
  } finally {
    loading.value = false
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await getCourseCategories(type.value)
  } catch {
    /* 忽略：分类加载失败不影响列表 */
  }
}

/** 模块切换时重置所有条件并重新加载 */
watch(type, () => {
  filters.tech_system_id = null
  filters.tech_direction_id = null
  filters.level = null
  filters.is_free = null
  filters.keyword = ''
  filters.sort_by = 'publish_time'
  pageNum.value = 1
  loadCategories()
  fetchCourses()
})

onMounted(() => {
  loadCategories()
  fetchCourses()
})
</script>

<template>
  <div class="course-center">
    <div class="page-head">
      <div class="crumbs">
        <span>课程中心</span>
        <i class="sep" aria-hidden="true">/</i>
        <span class="current">{{ moduleName }}</span>
      </div>

      <div class="toolbar">
        <div class="search-box">
          <label class="visually-hidden" for="course-search">搜索课程</label>
          <input
            id="course-search"
            v-model.trim="filters.keyword"
            class="form-input search-input"
            placeholder="搜索课程名称 / 讲师姓名"
            maxlength="50"
            @keyup.enter="doSearch"
          />
          <button class="btn btn-primary search-btn" type="button" @click="doSearch">搜索</button>
        </div>

        <div class="sort-tabs" role="group" aria-label="排序方式">
          <button
            v-for="opt in COURSE_SORT_OPTIONS"
            :key="opt.value"
            type="button"
            class="sort-tab"
            :class="{ active: filters.sort_by === opt.value }"
            @click="filters.sort_by = opt.value; onFilterChange()"
          >
            {{ opt.label }}
          </button>
        </div>
      </div>

      <div class="category-zone">
        <CategoryFilter
          :categories="categories"
          :model-value="categoryFilter"
          @update:model-value="onCategoryChange"
        />
      </div>

      <div class="filter-bar">
        <div class="filter-cell">
          <span class="filter-label">课程等级</span>
          <select v-model="levelSelect" class="filter-select" aria-label="按课程等级筛选" @change="onFilterChange">
            <option value="">全部等级</option>
            <option v-for="opt in COURSE_LEVEL_OPTIONS" :key="opt.value" :value="opt.value">
              {{ opt.label }}
            </option>
          </select>
        </div>
        <div class="filter-cell">
          <span class="filter-label">收费</span>
          <select v-model="freeSelect" class="filter-select" aria-label="按收费方式筛选" @change="onFilterChange">
            <option value="">全部</option>
            <option value="1">免费</option>
            <option value="0">付费</option>
          </select>
        </div>
        <div class="result-info">
          共 <b>{{ total }}</b> 门{{ moduleName }}
          <template v-if="hasKeyword">，关键词“{{ filters.keyword }}”</template>
          <template v-if="sortLabel && sortLabel !== '最新'">，按{{ sortLabel }}排序</template>
        </div>
      </div>
    </div>

    <section class="course-area" aria-live="polite">
        <div v-if="loading" class="course-grid">
          <div v-for="i in SKELETON_COUNT" :key="i" class="course-card">
            <div class="skeleton card-cover-skeleton" />
            <div class="card-line skeleton" />
            <div class="card-line short skeleton" />
            <div class="card-line foot skeleton" />
          </div>
        </div>

        <template v-else>
          <div v-if="courses.length === 0" class="empty-tip">
            <span class="empty-icon" aria-hidden="true">{{ hasKeyword ? '搜' : '空' }}</span>
            <p v-if="hasKeyword">没有找到与“{{ filters.keyword }}”相关的课程</p>
            <p v-else>该分类下暂无课程，试试切换到其他分类</p>
            <button v-if="hasKeyword" class="btn" type="button" @click="filters.keyword = ''; doSearch()">清空关键词</button>
          </div>
          <div v-else class="course-grid">
            <CourseCard v-for="course in courses" :key="course.id" :course="course" />
          </div>
        </template>

        <Pagination :current="pageNum" :total="total" :size="PAGE_SIZE" @change="changePage" />
    </section>
  </div>
</template>

<style scoped>
.course-center {
  min-width: 0;
}

.page-head {
  margin-bottom: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.category-zone {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.crumbs {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: 15px;
  color: var(--color-text-secondary);
}

.crumbs .sep {
  color: var(--color-border-strong);
}

.crumbs .current {
  color: var(--color-text);
  font-weight: 600;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-5);
  margin-top: var(--space-4);
}

.search-box {
  display: flex;
  gap: var(--space-2);
  width: 460px;
  max-width: 100%;
}

.search-input {
  height: 40px;
}

.search-btn {
  height: 40px;
  padding: 0 26px;
  flex-shrink: 0;
}

.sort-tabs {
  display: flex;
  padding: 3px;
  background: var(--color-bg);
  border-radius: 999px;
}

.sort-tab {
  padding: 7px 20px;
  border-radius: 999px;
  font-size: 14px;
  color: var(--color-text-secondary);
  transition: all var(--dur-fast) ease;
}

.sort-tab:hover {
  color: var(--color-primary);
}

.sort-tab.active {
  background: var(--color-card);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.filter-cell {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
}

.filter-label {
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.filter-select {
  height: 40px;
  min-width: 132px;
  padding: 0 28px 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  font-size: 13px;
  color: var(--color-text);
  outline: none;
  cursor: pointer;
  transition: border-color var(--dur-fast) ease;
}

.filter-select:hover {
  border-color: var(--color-border-strong);
}

.filter-select:focus {
  border-color: var(--color-primary);
}

.result-info {
  margin-left: auto;
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.result-info b {
  color: var(--color-primary);
  font-family: var(--font-num);
  font-size: 15px;
}

.course-area {
  min-width: 0;
}

.course-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(268px, 1fr));
  gap: var(--space-4);
}

/* 骨架占位卡片 */
.course-card {
  overflow: hidden;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.card-cover-skeleton {
  aspect-ratio: 16 / 9;
}

.card-line {
  height: 16px;
  margin: 14px 16px 0;
  border-radius: 4px;
}

.card-line.short {
  width: 60%;
}

.card-line.foot {
  height: 12px;
  margin-bottom: 16px;
  width: 40%;
}

@media (max-width: 860px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .search-box {
    width: 100%;
  }

  .sort-tabs {
    align-self: flex-start;
  }

  .result-info {
    margin-left: 0;
  }
}

@media (max-width: 560px) {
  .course-grid {
    grid-template-columns: 1fr;
  }

  .filter-bar {
    flex-wrap: wrap;
  }
}
</style>
