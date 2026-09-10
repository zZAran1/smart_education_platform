<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { getJobCategories, getJobPage } from '@/api/job'
import JobCard from '@/components/JobCard.vue'
import Pagination from '@/components/Pagination.vue'
import type { JobCardVO, JobCategoryVO } from '@/types/api'

const PAGE_SIZE = 10

const categories = ref<JobCategoryVO[]>([])
const jobs = ref<JobCardVO[]>([])
const total = ref(0)
const pageNum = ref(1)
const loading = ref(false)

const filters = reactive({
  system_id: null as number | null,
  category_id: null as number | null,
  search_type: 'job' as 'job' | 'company',
  keyword: '',
})

/** 当前选中的一级分类 */
const activeSystem = computed<JobCategoryVO | undefined>(() => {
  const id = filters.system_id
  if (id === null) return undefined
  return categories.value.find((c) => c.id === id)
})

const hasKeyword = computed(() => filters.keyword.trim().length > 0)

function selectAll(): void {
  filters.system_id = null
  filters.category_id = null
  resetAndFetch()
}

function selectSystem(system: JobCategoryVO): void {
  filters.system_id = system.id
  filters.category_id = null
  // 若一级下无二级分类，则直接按该一级过滤（后端 category_id 兼容）
  if (!system.children || system.children.length === 0) {
    filters.category_id = system.id
  }
  resetAndFetch()
}

function selectDirection(direction: JobCategoryVO): void {
  filters.category_id = direction.id
  resetAndFetch()
}

function changeSearchType(type: 'job' | 'company'): void {
  filters.search_type = type
  resetAndFetch()
}

function doSearch(): void {
  resetAndFetch()
}

function resetAndFetch(): void {
  pageNum.value = 1
  fetchJobs()
}

function changePage(p: number): void {
  pageNum.value = p
  fetchJobs()
}

async function fetchJobs(): Promise<void> {
  loading.value = true
  try {
    const data = await getJobPage({
      category_id: filters.category_id,
      search_type: filters.search_type,
      keyword: filters.keyword.trim(),
      page_num: pageNum.value,
      page_size: PAGE_SIZE,
    })
    jobs.value = data.records || []
    total.value = data.total || 0
  } catch {
    /* 错误已由拦截层提示 */
  } finally {
    loading.value = false
  }
}

async function loadCategories(): Promise<void> {
  try {
    categories.value = await getJobCategories()
  } catch {
    /* 忽略：分类加载失败不影响列表 */
  }
}

onMounted(() => {
  loadCategories()
  fetchJobs()
})
</script>

<template>
  <div class="job-center">
    <div class="page-head">
      <div class="crumbs">
        <span>实习就业</span>
        <i class="sep" aria-hidden="true">/</i>
        <span class="current">职位广场</span>
      </div>

      <div class="toolbar">
        <div class="search-box">
          <div class="search-type" role="group" aria-label="搜索类型">
            <button
              type="button"
              class="type-btn"
              :class="{ active: filters.search_type === 'job' }"
              @click="changeSearchType('job')"
            >
              职位
            </button>
            <button
              type="button"
              class="type-btn"
              :class="{ active: filters.search_type === 'company' }"
              @click="changeSearchType('company')"
            >
              公司
            </button>
          </div>
          <input
            v-model.trim="filters.keyword"
            class="form-input search-input"
            :placeholder="filters.search_type === 'job' ? '搜索职位名称' : '搜索公司名称'"
            maxlength="50"
            @keyup.enter="doSearch"
          />
          <button class="btn btn-primary search-btn" type="button" @click="doSearch">搜索</button>
        </div>
      </div>

      <div class="category-zone">
        <div class="chip-row">
          <span class="row-label">职位分类</span>
          <div class="chips">
            <button type="button" class="chip" :class="{ active: !filters.system_id }" @click="selectAll">
              全部职位
            </button>
            <button
              v-for="system in categories"
              :key="system.id"
              type="button"
              class="chip"
              :class="{ active: filters.system_id === system.id }"
              @click="selectSystem(system)"
            >
              {{ system.name }}
            </button>
          </div>
        </div>

        <div v-if="activeSystem && activeSystem.children?.length" class="chip-row direction">
          <span class="row-label">细分方向</span>
          <div class="chips">
            <button
              v-for="dir in activeSystem.children"
              :key="dir.id"
              type="button"
              class="chip"
              :class="{ active: filters.category_id === dir.id }"
              @click="selectDirection(dir)"
            >
              {{ dir.name }}
            </button>
          </div>
        </div>
      </div>

      <div class="result-info">
        共 <b>{{ total }}</b> 个职位
        <template v-if="hasKeyword">，关键词“{{ filters.keyword }}”</template>
      </div>
    </div>

    <section class="job-area" aria-live="polite">
      <div v-if="loading" class="job-list">
        <div v-for="i in 6" :key="i" class="skeleton skel-card" />
      </div>

      <template v-else>
        <div v-if="jobs.length === 0" class="empty-tip">
          <span class="empty-icon" aria-hidden="true">{{ hasKeyword ? '搜' : '职' }}</span>
          <p v-if="hasKeyword">没有找到与“{{ filters.keyword }}”相关的职位</p>
          <p v-else>该分类下暂无职位，试试切换到其他分类</p>
        </div>
        <div v-else class="job-list">
          <JobCard v-for="job in jobs" :key="job.id" :job="job" />
        </div>
      </template>

      <Pagination :current="pageNum" :total="total" :size="PAGE_SIZE" @change="changePage" />
    </section>
  </div>
</template>

<style scoped>
.job-center {
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
  margin-top: var(--space-4);
}

.search-box {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  max-width: 640px;
}

.search-type {
  display: flex;
  padding: 3px;
  background: var(--color-bg);
  border-radius: 999px;
  flex-shrink: 0;
}

.type-btn {
  padding: 6px 16px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: all var(--dur-fast) ease;
}

.type-btn.active {
  background: var(--color-card);
  color: var(--color-primary);
  font-weight: 600;
  box-shadow: var(--shadow-sm);
}

.search-input {
  flex: 1;
  min-width: 0;
  height: 40px;
}

.search-btn {
  height: 40px;
  flex-shrink: 0;
}

.category-zone {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
}

.chip-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.chip-row.direction {
  margin-top: var(--space-2);
}

.row-label {
  flex-shrink: 0;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.05em;
  color: var(--color-text-tertiary);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.chip {
  display: inline-flex;
  align-items: center;
  height: 30px;
  padding: 0 14px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  transition: border-color var(--dur-fast) ease, color var(--dur-fast) ease,
    background var(--dur-fast) ease;
}

.chip:hover {
  border-color: var(--color-primary-border);
  color: var(--color-primary);
}

.chip.active {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.result-info {
  margin-top: var(--space-4);
  color: var(--color-text-tertiary);
  font-size: 13px;
}

.result-info b {
  color: var(--color-primary);
  font-family: var(--font-num);
  font-size: 15px;
}

.job-area {
  min-width: 0;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.skel-card {
  height: 128px;
  border-radius: var(--radius-md);
}

@media (max-width: 640px) {
  .search-box {
    flex-wrap: wrap;
  }

  .chip-row {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
