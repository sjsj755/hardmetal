import { request } from './http'
import type {
  ContactPayload,
  ContactView,
  CreatedCustomer,
  CustomerDetailView,
  CustomerUpsertPayload,
  CustomerView,
  PageResult,
  TimelineResult
} from './types'

export interface CustomerQuery {
  page: number
  size: number
  type?: string
  country?: string
  level?: string
  ownerId?: number
  keyword?: string
}

export interface TimelineQuery {
  type?: string
  page: number
}

/**
 * GET /api/v1/customers（《接口文档》3.1）。
 * 空字符串不发送，避免后端把空串当成筛选条件。
 */
export function fetchCustomers(query: CustomerQuery): Promise<PageResult<CustomerView>> {
  const params: Record<string, string | number> = {
    page: query.page,
    size: query.size
  }
  if (query.type) {
    params.type = query.type
  }
  if (query.country) {
    params.country = query.country.toUpperCase()
  }
  if (query.level) {
    params.level = query.level
  }
  if (query.ownerId !== undefined) {
    params.ownerId = query.ownerId
  }
  if (query.keyword) {
    params.keyword = query.keyword
  }
  return request<PageResult<CustomerView>>({
    url: '/customers',
    method: 'get',
    params
  })
}

/** GET /api/v1/customers/{id}（3.3）：档案 + 联系人 + 价格摘要 + 最近报价 + 订单统计。 */
export function fetchCustomer(id: number | string): Promise<CustomerDetailView> {
  return request<CustomerDetailView>({
    url: `/customers/${id}`,
    method: 'get'
  })
}

/** POST /api/v1/customers（3.2）。 */
export function createCustomer(payload: CustomerUpsertPayload): Promise<CreatedCustomer> {
  return request<CreatedCustomer>({
    url: '/customers',
    method: 'post',
    data: payload
  })
}

/** PUT /api/v1/customers/{id}（3.4）：部分更新。 */
export function updateCustomer(id: number, payload: CustomerUpsertPayload): Promise<void> {
  return request<void>({
    url: `/customers/${id}`,
    method: 'put',
    data: payload
  })
}

/** DELETE /api/v1/customers/{id}（3.5）：软删除，存在未结业务返回 40904。 */
export function deleteCustomer(id: number): Promise<void> {
  return request<void>({
    url: `/customers/${id}`,
    method: 'delete'
  })
}

/** GET /api/v1/customers/{id}/contacts（契约外补充接口）。 */
export function fetchContacts(id: number): Promise<ContactView[]> {
  return request<ContactView[]>({
    url: `/customers/${id}/contacts`,
    method: 'get'
  })
}

/** POST /api/v1/customers/{id}/contacts（契约外补充接口）。 */
export function addContact(id: number, payload: ContactPayload): Promise<ContactView> {
  return request<ContactView>({
    url: `/customers/${id}/contacts`,
    method: 'post',
    data: payload
  })
}

/** DELETE /api/v1/customers/{id}/contacts/{contactId}（契约外补充接口）。 */
export function deleteContact(id: number, contactId: number): Promise<void> {
  return request<void>({
    url: `/customers/${id}/contacts/${contactId}`,
    method: 'delete'
  })
}

/** GET /api/v1/customers/{id}/timeline（3.6）：服务端每页固定 50 条，不返回 total。 */
export function fetchTimeline(id: number, query: TimelineQuery): Promise<TimelineResult> {
  const params: Record<string, string | number> = { page: query.page }
  if (query.type) {
    params.type = query.type
  }
  return request<TimelineResult>({
    url: `/customers/${id}/timeline`,
    method: 'get',
    params
  })
}