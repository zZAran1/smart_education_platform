<script setup lang="ts">
import { computed } from 'vue'
import type { CourseCategoryVO } from '@/types/api'

interface FilterValue {
  tech_system_id?: number | null
  tech_direction_id?: number | null
}

const props = defineProps<{
  /** 一级（技术体系）分类列表 */
  categories: CourseCategoryVO[]
  modelValue: FilterValue
}>()

const emit = defineEmits<{ 'update:modelValue': [value: FilterValue] }>()

/** 当前选中的一级分类对象 */
const activeSystem = computed<CourseCategoryVO | undefined>(() => {
  const id = props.modelValue.tech_system_id
  if (id === null || id === undefined) return undefined
  return props.categories.find((s) => s.id === id)
})

const activeDirectionId = computed(() => props.modelValue.tech_direction_id ?? null)

function selectAll(): void {
  emit('update:modelValue', { tech_system_id: null, tech_direction_id: null })
}

function selectSystem(system: CourseCategoryVO): void {
  emit('update:modelValue', {
    tech_system_id: system.id,
    tech_direction_id: null,
  })
}

function selectDirection(systemId: number, directionId: number): void {
  emit('update:modelValue', {
    tech_system_id: systemId,
    tech_direction_id: directionId,
  })
}
</script>

<template>
  <div class="category-filter" aria-label="按技术分类筛选">
    <div class="chip-row">
      <span class="row-label">技术分类</span>
      <div class="chips">
        <button
          type="button"
          class="chip"
          :class="{ active: !modelValue.tech_system_id }"
          @click="selectAll"
        >
          全部课程
        </button>
        <button
          v-for="system in categories"
          :key="system.id"
          type="button"
          class="chip"
          :class="{ active: modelValue.tech_system_id === system.id }"
          @click="selectSystem(system)"
        >
          {{ system.name }}
        </button>
      </div>
    </div>

    <div v-if="activeSystem && activeSystem.children?.length" class="chip-row direction">
      <span class="row-label">技术方向</span>
      <div class="chips">
        <button
          v-for="dir in activeSystem.children"
          :key="dir.id"
          type="button"
          class="chip"
          :class="{ active: activeDirectionId === dir.id }"
          @click="selectDirection(activeSystem!.id, dir.id)"
        >
          {{ dir.name }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.category-filter {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.chip-row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
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

@media (max-width: 640px) {
  .chip-row {
    align-items: flex-start;
    flex-direction: column;
    gap: var(--space-2);
  }
}
</style>
