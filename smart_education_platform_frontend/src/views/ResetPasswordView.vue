<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { sendResetCode, resetPassword } from '@/api/user'
import { showToast } from '@/composables/toast'

const router = useRouter()

const form = reactive({
  target: '',
  code: '',
  new_password: '',
  confirm_password: '',
})

const errors = reactive({ target: '', code: '', new_password: '', confirm_password: '' })
const sentCode = ref('')
const sending = ref(false)
const loading = ref(false)

async function onSendCode(): Promise<void> {
  if (!form.target.trim()) {
    errors.target = '请输入邮箱或手机号'
    return
  }
  errors.target = ''
  sending.value = true
  try {
    const data = await sendResetCode(form.target.trim())
    // 开发环境验证码直接回传，自动填入便于联调
    sentCode.value = data.code
    form.code = data.code
    showToast('验证码已发送（开发环境直接回显）', 'success')
  } catch {
    /* 已提示 */
  } finally {
    sending.value = false
  }
}

function validate(): boolean {
  errors.target = form.target ? '' : '请输入邮箱或手机号'
  errors.code = form.code ? '' : '请输入验证码'
  errors.new_password = form.new_password.length >= 6 ? '' : '密码长度须为6~32位'
  errors.confirm_password = form.confirm_password === form.new_password ? '' : '两次输入的密码不一致'
  return !errors.target && !errors.code && !errors.new_password && !errors.confirm_password
}

async function submit(): Promise<void> {
  if (!validate() || loading.value) return
  loading.value = true
  try {
    await resetPassword({
      target: form.target.trim(),
      code: form.code,
      new_password: form.new_password,
      confirm_password: form.confirm_password,
    })
    showToast('密码重置成功，请登录', 'success')
    router.push('/login')
  } catch {
    /* 已提示 */
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
        <h1 class="auth-title">重置密码</h1>
        <p class="auth-subtitle">通过邮箱或手机号验证码重置登录密码</p>
      </div>

      <div class="form-item">
        <label for="reset-target">邮箱 / 手机号</label>
        <div class="send-row">
          <input
            id="reset-target"
            v-model.trim="form.target"
            class="form-input"
            :class="{ 'is-invalid': errors.target }"
            placeholder="请输入注册时的邮箱或手机号"
            maxlength="64"
          />
          <button class="btn send-btn" type="button" :disabled="sending" @click="onSendCode">
            {{ sending ? '发送中…' : '获取验证码' }}
          </button>
        </div>
        <p v-if="errors.target" class="form-error" role="alert">{{ errors.target }}</p>
        <p v-if="sentCode" class="code-hint">验证码（开发环境回显）：{{ sentCode }}</p>
      </div>

      <div class="form-item">
        <label for="reset-code">验证码</label>
        <input
          id="reset-code"
          v-model.trim="form.code"
          class="form-input"
          :class="{ 'is-invalid': errors.code }"
          placeholder="请输入验证码"
          maxlength="6"
          autocomplete="off"
        />
        <p v-if="errors.code" class="form-error" role="alert">{{ errors.code }}</p>
      </div>

      <div class="form-item">
        <label for="reset-pwd">新密码</label>
        <input
          id="reset-pwd"
          v-model="form.new_password"
          class="form-input"
          :class="{ 'is-invalid': errors.new_password }"
          type="password"
          placeholder="6~32位"
          maxlength="32"
          autocomplete="new-password"
        />
        <p v-if="errors.new_password" class="form-error" role="alert">{{ errors.new_password }}</p>
      </div>

      <div class="form-item">
        <label for="reset-confirm">确认密码</label>
        <input
          id="reset-confirm"
          v-model="form.confirm_password"
          class="form-input"
          :class="{ 'is-invalid': errors.confirm_password }"
          type="password"
          placeholder="再次输入"
          maxlength="32"
          autocomplete="new-password"
          @keyup.enter="submit"
        />
        <p v-if="errors.confirm_password" class="form-error" role="alert">{{ errors.confirm_password }}</p>
      </div>

      <button class="btn btn-primary btn-block" type="button" :disabled="loading" @click="submit">
        <span v-if="loading" class="spinner" aria-hidden="true" />
        {{ loading ? '提交中…' : '重置密码' }}
      </button>

      <p class="auth-link">
        想起密码了？
        <RouterLink to="/login">返回登录</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.send-row {
  display: flex;
  gap: var(--space-2);
}

.send-row .form-input {
  flex: 1;
  min-width: 0;
}

.send-btn {
  height: 44px;
  flex-shrink: 0;
  color: var(--color-primary);
  border-color: var(--color-primary-border);
  box-shadow: none;
}

.code-hint {
  font-size: 12px;
  color: var(--color-warning);
}
</style>
