import { request } from './http'
import type { LoginPayload, LoginResult, UserProfile } from './types'

export function login(payload: LoginPayload): Promise<LoginResult> {
  return request<LoginResult>({
    url: '/auth/login',
    method: 'post',
    data: payload
  })
}

export function fetchMe(): Promise<UserProfile> {
  return request<UserProfile>({
    url: '/auth/me',
    method: 'get'
  })
}

export function logout(refreshToken: string | null): Promise<void> {
  return request<void>({
    url: '/auth/logout',
    method: 'post',
    data: refreshToken ? { refreshToken } : {}
  })
}