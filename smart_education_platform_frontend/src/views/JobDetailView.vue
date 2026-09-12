<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getJobDetail, toggleJobCollect, applyJob, applyAiInterview } from '@/api/job'
import { isLoggedIn } from '@/stores/auth'
import { showToast } from '@/composables/toast'
import { formatSalary, type JobDetailVO } from '@/types/api'

const route = useRoute()
const router = useRouter()

const job = ref<JobDetailVO | null>(null)
const loading = ref(true)
const collectLoading = ref(false)
const applyLoading = ref(false)
const interviewLoading = ref(false)

const jobId = computed(() => Number(route.params.id))
const salary = computed(() => formatSalary(job.value?.salary_min, job.value?.salary_max))
const title = computed(() => (job.value?.city ? `${job.value.city} · ${job.value?.title}` : job.value?.title || ''))
const logoText = computed(() => (job.value?.company_name || '企').trim().charAt(0).toUpperCase())

async function fetchDetail(): Promise<void> {
  loading.value = true
  try {
    job.value = await getJobDetail(jobId.value)
  } catch {
    router.replace('/job')
  } finally {
    loading.value = false
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
    const collected = await toggleJobCollect(jobId.value)
    if (job.value) job.value.is_collected = collected
    showToast(collected ? '已收藏' : '已取消收藏', 'success')
  } catch {
    /* 已提示 */
  } finally {
    collectLoading.value = false
  }
}

async function onApply(): Promise<void> {
  if (!requireLogin() || applyLoading.value) return
  applyLoading.value = true
  try {
    await applyJob(jobId.value)
    showToast('投递成功，可在个人中心「我的投递」查看', 'success')
    await fetchDetail()
  } catch {
    /* 已提示 */
  } finally {
    applyLoading.value = false
  }
}

async function onInterview(): Promise<void> {
  if (!requireLogin() || interviewLoading.value) return
  interviewLoading.value = true
  try {
    const id = await applyAiInterview(jobId.value)
    showToast(`已申请数字人面试（会话 ${id}）`, 'success')
  } catch {
    /* 已提示 */
  } finally {
    interviewLoading.value = false
  }
}

watch(jobId, fetchDetail, { immediate: true })
</script>

<template>
  <div class="container job-detail">
    <div v-if="loading" class="skeleton-wrap">
      <div class="skeleton skel-head" />
      <div class="skeleton skel-body" />
    </div>

    <template v-else-if="job">
      <nav class="crumbs" aria-label="面包屑">
        <RouterLink to="/job" class="link">实习就业</RouterLink>
        <i class="sep" aria-hidden="true">/</i>
        <span class="current" :title="title">{{ title }}</span>
      </nav>

      <div class="layout">
        <section class="main-col">
          <div class="panel job-head">
            <div class="head-top">
              <h1 class="title">{{ title }}</h1>
              <span class="salary">{{ salary }}</span>
            </div>
            <ul class="head-meta">
              <li v-if="job.address">工作地址：{{ job.address }}</li>
              <li>招聘人数：{{ job.headcount }} 人</li>
            </ul>
            <div class="head-actions">
              <button class="btn" :class="{ on: job.is_collected }" type="button" :disabled="collectLoading" @click="onCollect">
                {{ job.is_collected ? '已感兴趣' : '感兴趣' }}
              </button>
              <button class="btn btn-primary" type="button" :disabled="applyLoading || job.is_applied" @click="onApply">
                {{ job.is_applied ? '已投递' : '申请职位' }}
              </button>
              <button class="btn interview-btn" type="button" :disabled="interviewLoading || !job.is_applied" @click="onInterview">
                数字人面试
              </button>
            </div>
          </div>

          <div class="panel">
            <h2 class="panel-title">职位描述</h2>
            <p class="para">{{ job.description || '暂无职位描述' }}</p>
            <h2 class="panel-title">任职要求</h2>
            <p class="para">{{ job.requirement || '暂无任职要求' }}</p>
          </div>
        </section>

        <aside class="side-col">
          <div class="panel company-card">
            <div class="company-head">
              <span class="company-logo" aria-hidden="true">{{ logoText }}</span>
              <h2 class="company-name">{{ job.company_name || '公司待定' }}</h2>
            </div>
            <ul class="company-meta">
              <li v-if="job.company_industry"><span>行业</span>{{ job.company_industry }}</li>
              <li v-if="job.company_scale"><span>规模</span>{{ job.company_scale }}</li>
              <li v-if="job.company_region"><span>地区</span>{{ job.company_region }}</li>
            </ul>
            <p class="company-intro">{{ job.company_intro || '该公司暂未填写简介。' }}</p>
          </div>
        </aside>
      </div>
    </template>
  </div>
</template>

<style scoped>
.job-detail {
  min-width: 0;
}

.skeleton-wrap {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skel-head {
  height: 200px;
  border-radius: var(--radius-md);
}

.skel-body {
  height: 320px;
  border-radius: var(--radius-md);
}

.crumbs {
  display: flex;
  align-items: center;
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

.crumbs .current {
  max-width: 560px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: var(--color-text);
  font-weight: 500;
}

.layout {
  display: flex;
  align-items: flex-start;
  gap: var(--space-5);
}

.main-col {
  flex: 1;
  min-width: 0;
}

.side-col {
  width: 320px;
  flex-shrink: 0;
}

.panel {
  padding: var(--space-5);
  margin-bottom: var(--space-4);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
}

.job-head {
  margin-bottom: var(--space-4);
}

.head-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.title {
  flex: 1;
  min-width: 0;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.4;
  letter-spacing: var(--tracking-tight);
}

.salary {
  flex-shrink: 0;
  font-family: var(--font-num);
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.01em;
  color: var(--color-primary);
}

.head-meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-4);
  margin-top: var(--space-3);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.head-actions .btn.on {
  color: var(--color-primary);
  border-color: var(--color-primary-border);
  background: var(--color-primary-soft);
}

.interview-btn {
  color: var(--color-primary);
  border-color: var(--color-primary-border);
}

.panel-title {
  margin-bottom: var(--space-3);
  font-size: 16px;
  font-weight: 600;
}

.para {
  margin-bottom: var(--space-5);
  color: var(--color-text-secondary);
  line-height: 1.85;
  white-space: pre-wrap;
}

.para:last-child {
  margin-bottom: 0;
}

.company-card {
  position: sticky;
  top: 76px;
}

.company-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-4);
}

.company-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 20px;
  font-weight: 700;
}

.company-name {
  font-size: 17px;
  font-weight: 600;
}

.company-meta {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding-bottom: var(--space-4);
  margin-bottom: var(--space-4);
  border-bottom: 1px dashed var(--color-border);
}

.company-meta li {
  display: flex;
  gap: var(--space-3);
  font-size: 13px;
  color: var(--color-text-secondary);
}

.company-meta span {
  width: 34px;
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.company-intro {
  font-size: 13px;
  line-height: 1.8;
  color: var(--color-text-secondary);
}

@media (max-width: 860px) {
  .layout {
    flex-direction: column;
  }

  .side-col {
    width: 100%;
  }

  .company-card {
    position: static;
  }
}
</style>
