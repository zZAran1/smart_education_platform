<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCaptcha, loginUser } from '@/api/user'
import { setAuth } from '@/stores/auth'
import { showToast } from '@/composables/toast'
import type { CaptchaVO } from '@/types/api'

const router = useRouter()
const route = useRoute()

const form = reactive({
  username: (route.query.username as string) || '',
  password: '',
  uuid: '',
  captcha: '',
})

const errors = reactive({ username: '', password: '', captcha: '' })
const captchaImg = ref('')
const showPassword = ref(false)
const loading = ref(false)

async function refreshCaptcha(): Promise<void> {
  try {
    const data: CaptchaVO = await getCaptcha()
    form.uuid = data.uuid
    captchaImg.value = data.img_base64
    form.captcha = ''
  } catch {
    /* 网络异常已由拦截层提示 */
  }
}

function validate(): boolean {
  errors.username = form.username ? '' : '请输入账号'
  errors.password = form.password ? '' : '请输入密码'
  errors.captcha = form.captcha ? '' : '请输入验证码'
  return !errors.username && !errors.password && !errors.captcha
}

async function submit(): Promise<void> {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    const { token, user_vo } = await loginUser({
      username: form.username,
      password: form.password,
      uuid: form.uuid,
      captcha: form.captcha,
    })
    setAuth(token, user_vo)
    showToast(`欢迎回来，${user_vo.nickname || user_vo.username}`, 'success')
    router.push('/')
  } catch {
    // 验证码一次性：登录失败后强制刷新
    await refreshCaptcha().catch(() => {})
  } finally {
    loading.value = false
  }
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-mark" aria-hidden="true">智</span>
        <h1 class="auth-title">登录智慧教育平台</h1>
        <p class="auth-subtitle">学 · 练 · 证 · 就 一体化学习平台</p>
      </div>

      <div class="form-item">
        <label for="login-username">账号</label>
        <input
          id="login-username"
          v-model.trim="form.username"
          class="form-input"
          :class="{ 'is-invalid': errors.username }"
          placeholder="请输入用户名"
          maxlength="32"
          autocomplete="username"
        />
        <p v-if="errors.username" class="form-error" role="alert">{{ errors.username }}</p>
      </div>

      <div class="form-item">
        <label for="login-password">密码</label>
        <div class="pwd-wrap">
          <input
            id="login-password"
            v-model="form.password"
            class="form-input"
            :class="{ 'is-invalid': errors.password }"
            :type="showPassword ? 'text' : 'password'"
            placeholder="请输入密码"
            maxlength="32"
            autocomplete="current-password"
            @keyup.enter="submit"
          />
          <button
            class="pwd-toggle"
            type="button"
            :aria-label="showPassword ? '隐藏密码' : '显示密码'"
            @click="showPassword = !showPassword"
          >
            {{ showPassword ? '隐藏' : '显示' }}
          </button>
        </div>
        <p v-if="errors.password" class="form-error" role="alert">{{ errors.password }}</p>
      </div>

      <div class="form-item">
        <label for="login-captcha">验证码</label>
        <div class="captcha-row" :class="{ 'is-invalid': errors.captcha }">
          <input
            id="login-captcha"
            v-model.trim="form.captcha"
            class="form-input captcha-input"
            :class="{ 'is-invalid': errors.captcha }"
            placeholder="请输入图形验证码"
            maxlength="4"
            autocomplete="off"
            @keyup.enter="submit"
          />
          <button
            class="captcha-img"
            type="button"
            :aria-label="captchaImg ? '看不清？点击刷新验证码' : '加载验证码'"
            title="点击刷新验证码"
            @click="refreshCaptcha"
          >
            <!-- 后端 img_base64 已含 data:image/png;base64, 前缀，直接用作 src -->
            <img v-if="captchaImg" :src="captchaImg" alt="图形验证码" />
            <span v-else class="skeleton captcha-skeleton" />
          </button>
        </div>
        <p v-if="errors.captcha" class="form-error" role="alert">{{ errors.captcha }}</p>
      </div>

      <button class="btn btn-primary btn-block" type="button" :disabled="loading" @click="submit">
        <span v-if="loading" class="spinner" aria-hidden="true" />
        {{ loading ? '正在登录…' : '登 录' }}
      </button>

      <p class="auth-link">
        还没有账号？
        <RouterLink to="/register">立即注册</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
/* 密码框：显示/隐藏切换 */
.pwd-wrap {
  position: relative;
}

.pwd-wrap .form-input {
  padding-right: 56px;
}

.pwd-toggle {
  position: absolute;
  top: 50%;
  right: 6px;
  transform: translateY(-50%);
  padding: 4px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.pwd-toggle:hover {
  color: var(--color-primary);
  background: var(--color-primary-soft);
}

/* 验证码 */
.captcha-row {
  display: flex;
  gap: 10px;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img {
  flex-shrink: 0;
  width: 132px;
  height: 44px;
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: #fff;
  transition: border-color var(--dur-fast) ease, box-shadow var(--dur-fast) ease;
}

.captcha-img:hover {
  border-color: var(--color-primary-border);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-skeleton {
  display: block;
  width: 100%;
  height: 100%;
}
</style>
