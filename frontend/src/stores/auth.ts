import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchMe, login as loginApi, logout as logoutApi } from '../api/auth'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from '../api/token'
import type { LoginPayload, UserProfile } from '../api/types'

/**
 * 认证状态：token 只存 localStorage，用户资料与权限点存内存。
 * 权限点来自登录响应的 perms（按钮级依据），不做前端硬编码。
 */
export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getAccessToken())
  const user = ref<UserProfile | null>(null)

  const isLoggedIn = computed(() => Boolean(token.value))
  const perms = computed(() => user.value?.perms ?? [])
  const displayName = computed(
    () => user.value?.realName || user.value?.username || '未登录'
  )

  async function login(payload: LoginPayload): Promise<UserProfile> {
    const result = await loginApi(payload)
    setTokens({
      accessToken: result.accessToken,
      refreshToken: result.refreshToken
    })
    token.value = result.accessToken
    user.value = result.user
    return result.user
  }

  /** 刷新后重新拉取当前用户（用于页面刷新后恢复会话）。 */
  async function loadMe(): Promise<UserProfile | null> {
    if (!getAccessToken()) {
      reset()
      return null
    }
    const profile = await fetchMe()
    token.value = getAccessToken()
    user.value = profile
    return profile
  }

  async function logout(): Promise<void> {
    const refreshToken = getRefreshToken()
    try {
      await logoutApi(refreshToken)
    } catch {
      // 令牌已失效也继续本地登出，保证操作可完成
    }
    reset()
  }

  function reset(): void {
    clearTokens()
    token.value = null
    user.value = null
  }

  function hasPerm(perm: string): boolean {
    return perms.value.includes(perm)
  }

  function hasRole(...codes: string[]): boolean {
    return Boolean(user.value && codes.includes(user.value.roleCode))
  }

  return {
    token,
    user,
    isLoggedIn,
    perms,
    displayName,
    login,
    loadMe,
    logout,
    reset,
    hasPerm,
    hasRole
  }
})