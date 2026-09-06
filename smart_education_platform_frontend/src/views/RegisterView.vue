<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { registerUser } from '@/api/user'
import { showToast } from '@/composables/toast'

const router = useRouter()

const form = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirm_password: '',
})

const errors = reactive({
  username: '',
  email: '',
  phone: '',
  password: '',
  confirm_password: '',
})

const showPwd = ref(false)
const loading = ref(false)

function validate(): boolean {
  const emailReg = /^[\w.+-]+@[\w-]+(\.[\w-]+)+$/
  const phoneReg = /^1[3-9]\d{9}$/
  errors.username =
    form.username.length >= 4 && /^[a-zA-Z0-9_]+$/.test(form.username)
      ? ''
      : '用户名须为4~32位字母/数字/下划线'
  errors.email = !form.email || emailReg.test(form.email) ? '' : '邮箱格式不正确'
  errors.phone = !form.phone || phoneReg.test(form.phone) ? '' : '手机号格式不正确'
  errors.password = form.password.length >= 6 ? '' : '密码长度须为6~32位'
  errors.confirm_password = form.confirm_password === form.password ? '' : '两次输入的密码不一致'
  return !errors.username && !errors.email && !errors.phone && !errors.password && !errors.confirm_password
}

async function submit(): Promise<void> {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    await registerUser({
      username: form.username,
      password: form.password,
      confirm_password: form.confirm_password,
      email: form.email || undefined,
      phone: form.phone || undefined,
    })
    showToast('注册成功，请登录', 'success')
    router.push({ path: '/login', query: { username: form.username } })
  } catch {
    /* 业务/网络错误已由请求层统一提示 */
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <div class="auth-brand">
        <span class="brand-mark" aria-hidden="true">智</span>
        <h1 class="auth-title">注册账号</h1>
        <p class="auth-subtitle">用户名 + 密码即可注册，建议同时绑定邮箱或手机号</p>
      </div>

      <div class="form-item">
        <label for="reg-username">用户名</label>
        <input
          id="reg-username"
          v-model.trim="form.username"
          class="form-input"
          :class="{ 'is-invalid': errors.username }"
          placeholder="4~32位字母/数字/下划线"
          maxlength="32"
          autocomplete="username"
        />
        <p v-if="errors.username" class="form-error" role="alert">{{ errors.username }}</p>
      </div>

      <div class="form-item">
        <label for="reg-email">
          邮箱
          <span class="label-hint">选填 · 用于重置密码</span>
        </label>
        <input
          id="reg-email"
          v-model.trim="form.email"
          class="form-input"
          :class="{ 'is-invalid': errors.email }"
          type="email"
          placeholder="example@mail.com"
          maxlength="64"
          autocomplete="email"
        />
        <p v-if="errors.email" class="form-error" role="alert">{{ errors.email }}</p>
      </div>

      <div class="form-item">
        <label for="reg-phone">
          手机号
          <span class="label-hint">选填 · 用于重置密码</span>
        </label>
        <input
          id="reg-phone"
          v-model.trim="form.phone"
          class="form-input"
          :class="{ 'is-invalid': errors.phone }"
          placeholder="11 位手机号码"
          maxlength="11"
          autocomplete="tel"
        />
        <p v-if="errors.phone" class="form-error" role="alert">{{ errors.phone }}</p>
      </div>

      <div class="form-row">
        <div class="form-item pwd-col">
          <label for="reg-password">密码</label>
          <div class="pwd-wrap">
            <input
              id="reg-password"
              v-model="form.password"
              class="form-input"
              :class="{ 'is-invalid': errors.password }"
              :type="showPwd ? 'text' : 'password'"
              placeholder="6~32位"
              maxlength="32"
              autocomplete="new-password"
            />
          </div>
          <p v-if="errors.password" class="form-error" role="alert">{{ errors.password }}</p>
        </div>
        <div class="form-item pwd-col">
          <label for="reg-confirm">确认密码</label>
          <div class="pwd-wrap">
            <input
              id="reg-confirm"
              v-model="form.confirm_password"
              class="form-input"
              :class="{ 'is-invalid': errors.confirm_password }"
              :type="showPwd ? 'text' : 'password'"
              placeholder="再次输入"
              maxlength="32"
              autocomplete="new-password"
              @keyup.enter="submit"
            />
          </div>
          <p v-if="errors.confirm_password" class="form-error" role="alert">{{ errors.confirm_password }}</p>
        </div>
      </div>

      <label class="pwd-visibility">
        <input v-model="showPwd" type="checkbox" />
        <span>显示密码</span>
      </label>

      <button class="btn btn-primary btn-block" type="button" :disabled="loading" @click="submit">
        <span v-if="loading" class="spinner" aria-hidden="true" />
        {{ loading ? '正在注册…' : '注 册' }}
      </button>

      <p class="auth-link">
        已有账号？
        <RouterLink to="/login">去登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.form-row {
  display: flex;
  gap: 12px;
}

.pwd-col {
  flex: 1;
  min-width: 0;
}

.pwd-visibility {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin: -6px 0 var(--space-3);
  font-size: 13px;
  color: var(--color-text-secondary);
  cursor: pointer;
  user-select: none;
}

.pwd-visibility input {
  accent-color: var(--color-primary);
}
</style>
