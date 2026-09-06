<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { COURSE_TYPE_OPTIONS } from '@/types/api'

const route = useRoute()

/** 当前模块下的路由类型；课程详情页不明确类型时不高亮任何项 */
const activeType = computed(() => {
  const m = route.path.match(/^\/course\/(\d)/)
  return m ? Number(m[1]) : null
})
</script>

<template>
  <aside class="side-nav" aria-label="课程中心导航">
    <p class="side-title">课程中心</p>
    <nav class="side-list">
      <RouterLink
        v-for="opt in COURSE_TYPE_OPTIONS"
        :key="opt.value"
        :to="`/course/${opt.value}`"
        class="side-link"
        :class="{ active: activeType === opt.value }"
      >
        <span class="side-dot" aria-hidden="true" />
        {{ opt.label }}
      </RouterLink>
    </nav>
    <p class="side-note">理论 · 实训 · 认证一体化学习</p>
  </aside>
</template>

<style scoped>
.side-nav {
  position: sticky;
  top: 76px;
  width: 208px;
  flex-shrink: 0;
  padding: var(--space-4) var(--space-2);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.side-title {
  padding: 0 var(--space-3) var(--space-2);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.06em;
  color: var(--color-text-tertiary);
}

.side-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.side-link {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px var(--space-3);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--color-text);
  transition: color var(--dur-fast) ease, background var(--dur-fast) ease;
}

.side-link:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

.side-link.active {
  color: var(--color-primary);
  font-weight: 600;
  background: var(--color-primary-soft);
}

.side-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--color-border-strong);
  flex-shrink: 0;
}

.side-link.active .side-dot,
.side-link:hover .side-dot {
  background: var(--color-primary);
}

.side-note {
  padding: var(--space-4) var(--space-3) var(--space-1);
  font-size: 12px;
  line-height: 1.6;
  color: var(--color-text-tertiary);
  border-top: 1px dashed var(--color-border);
  margin-top: var(--space-3);
}

@media (max-width: 960px) {
  .side-nav {
    display: none;
  }
}
</style>
