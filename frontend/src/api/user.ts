import { request } from './http'
import type { UserOptionsResult } from './types'

/**
 * GET /api/v1/users/options?role=SALES
 * 仅用于归属员工下拉，不用于完整用户管理；只允许 ADMIN 调用。
 */
export function fetchUserOptions(role = 'SALES'): Promise<UserOptionsResult> {
  return request<UserOptionsResult>({
    url: '/users/options',
    method: 'get',
    params: { role }
  })
}