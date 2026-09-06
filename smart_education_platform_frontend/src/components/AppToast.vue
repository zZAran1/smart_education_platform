<script setup lang="ts">
import { toastState, removeToast } from '@/composables/toast'
</script>

<template>
  <Teleport to="body">
    <div class="app-toast-wrap" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast">
        <button
          v-for="item in toastState.list"
          :key="item.id"
          class="app-toast"
          :class="`toast-${item.type}`"
          type="button"
          :aria-label="`关闭提示：${item.message}`"
          @click="removeToast(item.id)"
        >
          <span class="toast-dot" aria-hidden="true" />
          <span class="toast-msg">{{ item.message }}</span>
        </button>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.app-toast-wrap {
  position: fixed;
  top: 68px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 9999;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  pointer-events: none;
}

.app-toast {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  max-width: min(520px, 92vw);
  padding: 10px 18px;
  border-radius: 999px;
  background: #fff;
  color: var(--color-text);
  font-size: 14px;
  box-shadow: var(--shadow-md);
  pointer-events: auto;
  transition: transform var(--dur-base) var(--ease-out), box-shadow var(--dur-base) ease;
}

.app-toast:hover {
  transform: translateY(-1px);
  box-shadow: var(--shadow-lg);
}

.toast-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.toast-msg {
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.toast-success .toast-dot {
  background: var(--color-success);
  box-shadow: 0 0 0 3px var(--color-success-soft);
}

.toast-error .toast-dot {
  background: var(--color-danger);
  box-shadow: 0 0 0 3px var(--color-danger-soft);
}

.toast-info .toast-dot {
  background: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.toast-enter-active,
.toast-leave-active {
  transition: all 0.28s var(--ease-out);
}

.toast-enter-from {
  opacity: 0;
  transform: translateY(-14px) scale(0.96);
}

.toast-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.97);
}
</style>
