<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppHeader from '@/components/AppHeader.vue'
import AppToast from '@/components/AppToast.vue'
import SideNav from '@/components/SideNav.vue'

const route = useRoute()

/** 全屏页面（登录/注册）：无顶栏、无左侧导航 */
const isPlain = computed(() => !!route.meta.plain)
</script>

<template>
  <AppHeader v-if="!isPlain" />

  <div v-if="!isPlain" class="container layout">
    <SideNav />
    <main class="layout-main">
      <RouterView />
    </main>
  </div>

  <main v-else class="plain-main">
    <RouterView />
  </main>

  <AppToast />
</template>

<style scoped>
.layout {
  display: flex;
  align-items: flex-start;
  gap: var(--space-5);
  flex: 1;
  padding-top: var(--space-5);
  padding-bottom: var(--space-7);
}

.layout-main {
  flex: 1;
  min-width: 0;
}

.plain-main {
  display: flex;
  flex-direction: column;
  flex: 1;
}
</style>
