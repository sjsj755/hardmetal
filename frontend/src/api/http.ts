import axios, {
  type AxiosError,
  type AxiosRequestConfig,
  type InternalAxiosRequestConfig
} from 'axios'
import type { ApiEnvelope, RefreshResult } from './types'
import { clearTokens, getAccessToken, getRefreshToken, setTokens } from './token'

// 后端所有业务接口都在 /api/v1 下；Vite 代理 /api → localhost:8080
const API_BASE: string = import.meta.env.VITE_API_BASE ?? '/api/v1'
const TIMEOUT = 15_000

export interface ApiErrorDetail {
  field?: string
  reason?: string
}

/**
 * 业务错误：code 取后端错误码（《接口文档》0.6），message 取后端 msg。
 * 前端只按 code 分支，不依赖 HTTP 状态码。
 */
export class ApiError extends Error {
  readonly code: number
  readonly traceId?: string
  readonly detail?: ApiErrorDetail

  constructor(code: number, message: string, traceId?: string, detail?: ApiErrorDetail) {
    super(message)
    this.name = 'ApiError'
    this.code = code
    this.traceId = traceId
    this.detail = detail
  }
}

export function errorMessage(error: unknown): string {
  return error instanceof ApiError ? error.message : '操作失败，请稍后重试'
}

type RetriableConfig = InternalAxiosRequestConfig & { _retried?: boolean }

const http = axios.create({
  baseURL: API_BASE,
  timeout: TIMEOUT
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

/**
 * 40102 刷新单例：并发请求必须共用同一个 promise（《接口文档》2.2）。
 */
let refreshPromise: Promise<string> | null = null

async function doRefresh(): Promise<string> {
  const refreshToken = getRefreshToken()
  if (!refreshToken) {
    throw new ApiError(40101, '登录已失效，请重新登录')
  }
  const response = await axios.post<ApiEnvelope<RefreshResult>>(
    `${API_BASE}/auth/refresh`,
    { refreshToken },
    { timeout: TIMEOUT }
  )
  const body = response.data
  if (!body || body.code !== 0) {
    throw new ApiError(body?.code ?? -1, body?.msg ?? '刷新令牌失败', body?.traceId)
  }
  setTokens({
    accessToken: body.data.accessToken,
    refreshToken: body.data.refreshToken
  })
  return body.data.accessToken
}

function refreshAccessToken(): Promise<string> {
  if (!refreshPromise) {
    refreshPromise = doRefresh().finally(() => {
      refreshPromise = null
    })
  }
  return refreshPromise
}

function isAuthEndpoint(url?: string): boolean {
  return Boolean(url && (url.includes('/auth/login') || url.includes('/auth/refresh')))
}

function expireSession(): void {
  clearTokens()
  if (typeof window === 'undefined' || window.location.pathname === '/login') {
    return
  }
  const redirect = `${window.location.pathname}${window.location.search}`
  window.location.replace(`/login?expired=1&redirect=${encodeURIComponent(redirect)}`)
}

function detailOf(data: unknown): ApiErrorDetail {
  if (data && typeof data === 'object') {
    const record = data as Record<string, unknown>
    return {
      field: typeof record.field === 'string' ? record.field : undefined,
      reason: typeof record.reason === 'string' ? record.reason : undefined
    }
  }
  return {}
}

http.interceptors.response.use(
  (response) => {
    const body = response.data as ApiEnvelope<unknown> | undefined
    if (!body || typeof body.code !== 'number') {
      return Promise.reject(new ApiError(-1, '响应格式异常，请确认接口地址'))
    }
    if (body.code !== 0) {
      return Promise.reject(
        new ApiError(body.code, body.msg, body.traceId, detailOf(body.data))
      )
    }
    return response
  },
  async (error: AxiosError<ApiEnvelope<unknown>>) => {
    const body = error.response?.data
    const traceId = body?.traceId
    const code = typeof body?.code === 'number' ? body.code : -1
    const message =
      body?.msg ??
      (error.code === 'ECONNABORTED'
        ? '请求超时，请稍后重试'
        : '网络异常，请确认后端服务已启动（默认 http://localhost:8080）')
    const config = error.config as RetriableConfig | undefined

    // access token 过期：刷新一次后重放原请求；失败则回登录页
    if (code === 40102 && config && !config._retried && !isAuthEndpoint(config.url)) {
      config._retried = true
      try {
        const accessToken = await refreshAccessToken()
        config.headers.Authorization = `Bearer ${accessToken}`
        return http.request(config)
      } catch {
        expireSession()
        throw new ApiError(40101, '登录已过期，请重新登录', traceId)
      }
    }

    if (code === 40101 || (code === 40102 && config?._retried)) {
      expireSession()
    }

    throw new ApiError(code, message, traceId, detailOf(body?.data))
  }
)

/** 发请求并解包 data；失败抛 ApiError。 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const response = await http.request<ApiEnvelope<T>>(config)
  return response.data.data
}