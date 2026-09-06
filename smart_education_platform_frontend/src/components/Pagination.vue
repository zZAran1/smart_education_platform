<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  current: number
  total: number
  size: number
}>()

const emit = defineEmits<{ change: [pageNum: number] }>()

const totalPages = computed(() => (props.size > 0 ? Math.max(1, Math.ceil(props.total / props.size)) : 1))

/** 页码序列：首尾固定，中间窗口 5 页，间隔用省略号表示 */
const items = computed<(number | '…')[]>(() => {
  const total = totalPages.value
  const current = props.current
  if (total <= 7) {
    return Array.from({ length: total }, (_, i) => i + 1)
  }
  const list: (number | '…')[] = [1]
  const start = Math.max(2, Math.min(current - 2, total - 5))
  const end = Math.min(start + 4, total - 1)
  if (start > 2) list.push('…')
  for (let p = start; p <= end; p += 1) list.push(p)
  if (end < total - 1) list.push('…')
  list.push(total)
  return list
})

const hasPrev = computed(() => props.current > 1)
const hasNext = computed(() => props.current < totalPages.value)

function change(pageNum: number): void {
  if (pageNum < 1 || pageNum > totalPages.value || pageNum === props.current) return
  emit('change', pageNum)
}
</script>

<template>
  <nav v-if="totalPages > 1" class="pagination" aria-label="分页导航">
    <button class="btn page-btn" type="button" :disabled="!hasPrev" aria-label="上一页" @click="change(current - 1)">
      ‹
    </button>

    <template v-for="(item, index) in items" :key="index">
      <span v-if="item === '…'" class="ellipsis" aria-hidden="true">…</span>
      <button
        v-else
        type="button"
        class="num-btn"
        :class="{ current: item === current }"
        :aria-current="item === current ? 'page' : undefined"
        @click="change(item)"
      >
        {{ item }}
      </button>
    </template>

    <button class="btn page-btn" type="button" :disabled="!hasNext" aria-label="下一页" @click="change(current + 1)">
      ›
    </button>
  </nav>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: var(--space-5) 0 var(--space-2);
}

.page-btn {
  width: 36px;
  height: 36px;
  padding: 0;
  font-size: 16px;
  box-shadow: none;
}

.num-btn {
  min-width: 36px;
  height: 36px;
  padding: 0 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text);
  font-size: 14px;
  font-family: var(--font-num);
  transition: all var(--dur-fast) ease;
}

.num-btn:hover {
  border-color: var(--color-primary-border);
  color: var(--color-primary);
}

.num-btn.current {
  border-color: var(--color-primary);
  background: var(--color-primary);
  color: #fff;
  font-weight: 600;
}

.ellipsis {
  padding: 0 2px;
  color: var(--color-text-tertiary);
}
</style>
