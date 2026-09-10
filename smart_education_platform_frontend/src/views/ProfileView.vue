<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { getProfile, updateProfile, updateAvatar } from '@/api/user'
import { authState, setUserInfo } from '@/stores/auth'
import { showToast } from '@/composables/toast'
import { ROLE_MAP, type UserVO } from '@/types/api'

const user = ref<UserVO | null>(null)
const loading = ref(true)
const saving = ref(false)
const uploading = ref(false)
const fileInput = ref<HTMLInputElement>()

function triggerUpload(): void {
  fileInput.value?.click()
}

const form = reactive({
  nickname: '',
  real_name: '',
})

async function fetchProfile(): Promise<void> {
  loading.value = true
  try {
    const data = await getProfile()
    user.value = data
    form.nickname = data.nickname || ''
    form.real_name = data.real_name || ''
    setUserInfo(data)
  } catch {
    /* 已提示 */
  } finally {
    loading.value = false
  }
}

async function onSave(): Promise<void> {
  if (saving.value) return
  saving.value = true
  try {
    const data = await updateProfile({
      nickname: form.nickname.trim(),
      real_name: form.real_name.trim(),
    })
    user.value = data
    setUserInfo(data)
    showToast('资料已更新', 'success')
  } catch {
    /* 已提示 */
  } finally {
    saving.value = false
  }
}

async function onAvatarChange(e: Event): Promise<void> {
  const input = e.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file || uploading.value) return
  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    showToast('头像仅支持 JPG/PNG', 'error')
    return
  }
  if (file.size > 2 * 1024 * 1024) {
    showToast('头像大小不能超过 2MB', 'error')
    return
  }
  uploading.value = true
  try {
    const avatar = await updateAvatar(file)
    if (user.value) user.value.avatar = avatar
    if (authState.userInfo) authState.userInfo.avatar = avatar
    showToast('头像已更新', 'success')
  } catch {
    /* 已提示 */
  } finally {
    uploading.value = false
    input.value = ''
  }
}

const roleText = (role?: number) => ROLE_MAP.get(role ?? 0) || '学员'

onMounted(fetchProfile)
</script>

<template>
  <div class="profile-page">
    <div v-if="loading" class="skeleton skel-card" />

    <template v-else-if="user">
      <div class="profile-card">
        <div class="profile-head">
          <button class="avatar-btn" type="button" :disabled="uploading" title="点击更换头像" @click="triggerUpload">
            <img v-if="user.avatar" :src="user.avatar ?? ''" :alt="user.nickname || '头像'" class="avatar" />
            <span v-else class="avatar avatar-fallback">{{ (user.nickname || user.username).charAt(0).toUpperCase() }}</span>
            <span class="avatar-mask" aria-hidden="true">{{ uploading ? '上传中…' : '更换' }}</span>
          </button>
          <input ref="fileInput" type="file" accept="image/png,image/jpeg" class="visually-hidden" @change="onAvatarChange" />

          <div class="profile-title">
            <h1 class="name">{{ user.nickname || user.username }}</h1>
            <p class="account">@{{ user.username }} · {{ roleText(user.role) }}</p>
          </div>
        </div>

        <div class="profile-body">
          <div class="form-grid">
            <div class="form-item">
              <label for="pf-nickname">昵称</label>
              <input id="pf-nickname" v-model.trim="form.nickname" class="form-input" maxlength="32" placeholder="设置昵称" />
            </div>
            <div class="form-item">
              <label for="pf-realname">姓名</label>
              <input id="pf-realname" v-model.trim="form.real_name" class="form-input" maxlength="32" placeholder="真实姓名" />
            </div>
          </div>

          <dl class="info-list">
            <div class="info-item">
              <dt>邮箱</dt>
              <dd>{{ user.email || '未绑定' }}</dd>
            </div>
            <div class="info-item">
              <dt>手机号</dt>
              <dd>{{ user.phone || '未绑定' }}</dd>
            </div>
            <div class="info-item">
              <dt>注册时间</dt>
              <dd>{{ user.created_at ? user.created_at.slice(0, 10) : '—' }}</dd>
            </div>
          </dl>

          <div class="actions">
            <button class="btn btn-primary" type="button" :disabled="saving" @click="onSave">
              {{ saving ? '保存中…' : '保存资料' }}
            </button>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.profile-page {
  min-width: 0;
  max-width: 760px;
}

.skel-card {
  height: 360px;
  border-radius: var(--radius-md);
}

.profile-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  overflow: hidden;
}

.profile-head {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-6);
  background: linear-gradient(180deg, var(--color-primary-soft) 0%, transparent 100%);
}

.avatar-btn {
  position: relative;
  width: 72px;
  height: 72px;
  flex-shrink: 0;
  border-radius: 50%;
  overflow: hidden;
}

.avatar {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-primary);
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.avatar-mask {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(23, 35, 61, 0.5);
  color: #fff;
  font-size: 12px;
  opacity: 0;
  transition: opacity var(--dur-fast) ease;
}

.avatar-btn:hover .avatar-mask {
  opacity: 1;
}

.profile-title {
  min-width: 0;
}

.name {
  font-size: 22px;
  font-weight: 700;
}

.account {
  margin-top: 2px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.profile-body {
  padding: var(--space-5) var(--space-6) var(--space-6);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-4);
}

.info-list {
  display: flex;
  flex-direction: column;
  margin-top: var(--space-5);
  padding-top: var(--space-4);
  border-top: 1px dashed var(--color-border);
}

.info-item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-2) 0;
}

.info-item dt {
  width: 80px;
  flex-shrink: 0;
  font-size: 13px;
  color: var(--color-text-tertiary);
}

.info-item dd {
  margin: 0;
  font-size: 14px;
  color: var(--color-text);
}

.actions {
  display: flex;
  justify-content: flex-end;
  margin-top: var(--space-5);
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: 1fr;
  }

  .profile-head {
    flex-direction: column;
    text-align: center;
  }
}
</style>
