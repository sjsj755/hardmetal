<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import type { FormInstance, FormRules } from 'element-plus'
import { ElMessage } from 'element-plus'
import { Lock, User } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { ApiError } from '../api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const formRef = ref<FormInstance>()
const form = reactive({ username: '', password: '' })
const submitting = ref(false)
const errorText = ref('')

const rules: FormRules = {
  username: [{ required: true, message: '请输入登录名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const expired = computed(() => route.query.expired === '1')

/** 只允许站内跳转，避免登录后被带到外部地址。 */
function safeRedirect(value: unknown): string {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//')
    ? value
    : '/dashboard'
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  errorText.value = ''
  try {
    await auth.login({ username: form.username.trim(), password: form.password })
    ElMessage.success('登录成功')
    await router.replace(safeRedirect(route.query.redirect))
  } catch (error) {
    errorText.value = error instanceof ApiError ? error.message : '登录失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <section class="intro">
        <div class="intro-brand">
          <span class="intro-mark">HM</span>
          <span class="intro-brand-text">Cemented Carbide Export Suite</span>
        </div>

        <h1 class="intro-title">
          硬质合金刀具<br />
          出口管理系统
        </h1>
        <p class="intro-sub">
          客户档案、联系人与跟进时间轴，<br />
          集中在一个干净的工作台。
        </p>

        <ul class="intro-points">
          <li>客户档案与联系人集中维护</li>
          <li>客户类型、等级与归属一目了然</li>
          <li>按角色控制可见范围与操作权限</li>
        </ul>

        <p class="intro-foot">内部业务系统 · 请勿共享账号</p>
      </section>

      <section class="form-panel">
        <h2 class="form-title">登录</h2>
        <p class="form-sub">使用内部账号登录</p>

        <el-alert
          v-if="expired"
          title="登录状态已失效，请重新登录"
          type="info"
          :closable="false"
          show-icon
          class="form-alert"
        />

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @submit.prevent
        >
          <el-form-item label="登录名" prop="username">
            <el-input
              v-model="form.username"
              size="large"
              :prefix-icon="User"
              placeholder="请输入登录名"
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              size="large"
              type="password"
              show-password
              :prefix-icon="Lock"
              placeholder="请输入密码"
              autocomplete="current-password"
              @keyup.enter="submit"
            />
          </el-form-item>

          <el-alert
            v-if="errorText"
            :title="errorText"
            type="error"
            :closable="false"
            show-icon
            class="form-alert"
          />

          <el-button
            type="primary"
            size="large"
            class="submit-btn"
            :loading="submitting"
            @click="submit"
          >
            登 录
          </el-button>
        </el-form>

        <p class="form-note">本系统仅限内部员工使用，不开放客户注册。</p>
      </section>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(900px 500px at 15% 10%, #eef4ff 0%, rgba(238, 244, 255, 0) 60%),
    radial-gradient(700px 400px at 90% 90%, #f0f7f3 0%, rgba(240, 247, 243, 0) 55%),
    var(--bg);
}

.login-card {
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  width: min(960px, 100%);
  overflow: hidden;
  background: var(--surface);
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 24px 64px rgba(31, 45, 74, 0.08);
}

.intro {
  display: flex;
  flex-direction: column;
  padding: 46px 44px 36px;
  background: linear-gradient(165deg, #f3f7ff 0%, #eaf1ff 52%, #e3ecff 100%);
}

.intro-brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.intro-mark {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: #ffffff;
  color: var(--primary);
  font-size: 12px;
  font-weight: 700;
  box-shadow: var(--shadow-sm);
}

.intro-brand-text {
  color: #6b7fa8;
  font-size: 10.5px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.intro-title {
  margin-top: 40px;
  color: #1c2b45;
  font-size: 34px;
  font-weight: 600;
  line-height: 1.32;
  letter-spacing: -0.01em;
}

.intro-sub {
  margin-top: 16px;
  color: #63758f;
  font-size: 13.5px;
  line-height: 1.85;
}

.intro-points {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 30px 0 0;
  padding: 0;
  list-style: none;
  color: #4c5f7c;
  font-size: 13px;
}

.intro-points li {
  display: flex;
  align-items: center;
  gap: 10px;
}

.intro-points li::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #9fb8e8;
}

.intro-foot {
  margin-top: auto;
  padding-top: 36px;
  color: #8a9bb4;
  font-size: 11.5px;
}

.form-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 46px 44px;
  background: var(--surface);
}

.form-title {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.form-sub {
  margin-top: 4px;
  margin-bottom: 24px;
  color: var(--text-2);
  font-size: 13px;
}

.form-alert {
  margin-bottom: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.form-note {
  margin-top: 18px;
  color: var(--text-3);
  font-size: 11.5px;
  line-height: 1.7;
}

@media (max-width: 820px) {
  .login-page {
    padding: 18px;
  }

  .login-card {
    grid-template-columns: minmax(0, 1fr);
  }

  .intro {
    padding: 30px 26px 24px;
  }

  .intro-title {
    margin-top: 22px;
    font-size: 26px;
  }

  .intro-points,
  .intro-foot {
    display: none;
  }

  .form-panel {
    padding: 30px 26px 34px;
  }
}
</style>