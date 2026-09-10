<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { formatSalary, type JobCardVO } from '@/types/api'

const props = defineProps<{ job: JobCardVO }>()

const router = useRouter()

const salary = computed(() => formatSalary(props.job.salary_min, props.job.salary_max))
const title = computed(() => (props.job.city ? `${props.job.city} · ${props.job.title}` : props.job.title))
const logoText = computed(() => (props.job.company_name || '企').trim().charAt(0).toUpperCase())

function goDetail(): void {
  router.push(`/job/detail/${props.job.id}`)
}
</script>

<template>
  <article
    class="job-card lift"
    role="button"
    tabindex="0"
    :aria-label="`查看职位：${job.title}`"
    @click="goDetail"
    @keydown.enter="goDetail"
  >
    <div class="job-top">
      <h3 class="job-title" :title="title">{{ title }}</h3>
      <span class="salary">{{ salary }}</span>
    </div>

    <div class="company">
      <span class="company-logo" aria-hidden="true">{{ logoText }}</span>
      <div class="company-meta">
        <p class="company-name">{{ job.company_name || '公司待定' }}</p>
        <p class="company-tags">
          <span v-if="job.company_industry">{{ job.company_industry }}</span>
          <span v-if="job.company_scale">{{ job.company_scale }}</span>
          <span v-if="job.company_region">{{ job.company_region }}</span>
        </p>
      </div>
    </div>

    <div class="job-foot">
      <span class="meta">招聘 {{ job.headcount }} 人</span>
      <span v-if="job.address" class="meta address" :title="job.address">{{ job.address }}</span>
    </div>
  </article>
</template>

<style scoped>
.job-card {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: pointer;
  box-shadow: var(--shadow-sm);
}

.job-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.job-title {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 17px;
  font-weight: 600;
  line-height: 1.4;
}

.salary {
  flex-shrink: 0;
  padding: 3px 12px;
  border-radius: 999px;
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 15px;
  font-weight: 700;
  font-family: var(--font-num);
}

.company {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.company-logo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 17px;
  font-weight: 700;
}

.company-meta {
  min-width: 0;
}

.company-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--color-text);
}

.company-tags {
  display: flex;
  gap: 6px;
  margin-top: 2px;
  overflow: hidden;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.company-tags span {
  white-space: nowrap;
}

.company-tags span + span::before {
  content: '·';
  margin-right: 6px;
}

.job-foot {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px dashed var(--color-border);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.job-foot .address {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
