<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { authState, clearAuth, isLoggedIn } from '@/stores/auth'
import { logout as logoutApi } from '@/api/user'
import { showToast } from '@/composables/toast'
import { ROLE } from '@/types/api'

const router = useRouter()
const route = useRoute()

const atCourse = computed(() => route.path.startsWith('/course'))
const atJob = computed(() => route.path.startsWith('/job'))
const isAdmin = computed(() => authState.userInfo?.role === ROLE.ADMIN)

const displayName = computed(
  () => authState.userInfo?.nickname || authState.userInfo?.username || '用户',
)

async function logout(): Promise<void> {
  try {
    await logoutApi()
  } catch {
    /* 即使后端失败也清除本地登录态 */
  }
  clearAuth()
  showToast('已退出登录', 'success')
  router.push('/login')
}
</script>

<template>
  <header class="app-header">
    <div class="container header-inner">
      <button class="brand" type="button" aria-label="返回课程中心" @click="router.push('/course/0')">
        <span class="brand-mark" aria-hidden="true">智</span>
        <span class="brand-name">智慧教育平台</span>
      </button>

      <nav class="nav" aria-label="一级导航">
        <RouterLink to="/course/0" class="nav-link" :class="{ active: atCourse }">课程中心</RouterLink>
        <RouterLink to="/job" class="nav-link" :class="{ active: atJob }">实习就业</RouterLink>
      </nav>

      <div class="header-right">
        <template v-if="isLoggedIn()">
          <button
            v-if="isAdmin"
            class="btn btn-sm admin-btn"
            type="button"
            @click="router.push('/admin')"
          >
            后台管理
          </button>
          <button class="user-chip" type="button" :title="`账号：${authState.userInfo?.username ?? ''}`" @click="router.push('/profile')">
            <span class="user-avatar" aria-hidden="true">{{ displayName.charAt(0).toUpperCase() }}</span>
            <span class="user-name">{{ displayName }}</span>
          </button>
          <button class="btn btn-sm" type="button" @click="logout">退出</button>
        </template>
        <template v-else>
          <button class="btn btn-sm" type="button" @click="router.push('/login')">登录</button>
          <button class="btn btn-primary btn-sm" type="button" @click="router.push('/register')">注册</button>
        </template>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: sticky;
  top: 0;
  z-index: 100;
  height: 60px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: saturate(1.4) blur(8px);
  border-bottom: 1px solid var(--color-border);
}

.header-inner {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  height: 60px;
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  padding: 4px;
  border-radius: var(--radius-md);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: var(--color-primary);
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  box-shadow: 0 6px 14px var(--color-primary-glow);
}

.brand-name {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.02em;
}

.nav {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  flex: 1;
}

.nav-link {
  position: relative;
  padding: 8px 2px;
  font-size: 15px;
  color: var(--color-text);
  transition: color var(--dur-fast) ease;
}

.nav-link::after {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  bottom: -1px;
  height: 2px;
  border-radius: 2px;
  background: transparent;
  transition: background var(--dur-fast) ease;
}

.nav-link:hover {
  color: var(--color-primary);
}

.nav-link.active {
  color: var(--color-primary);
  font-weight: 600;
}

.nav-link.active::after {
  background: var(--color-primary);
}

.header-right {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.admin-btn {
  color: var(--color-primary);
  border-color: var(--color-primary-border);
}

.user-chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 3px 10px 3px 3px;
  border-radius: 999px;
  border: 1px solid transparent;
  transition: background var(--dur-fast) ease;
}

.user-chip:hover {
  background: var(--color-bg);
}

.user-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}

.user-name {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: var(--color-text);
}

@media (max-width: 768px) {
  .brand-name {
    display: none;
  }

  .user-name {
    max-width: 72px;
  }
}
</style>
