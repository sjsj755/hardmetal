/**
 * 接口类型。字段与后端 DTO 保持一致：
 * - 统一响应 {code, msg, data, traceId}，code = 0 成功（《接口文档》0.3）
 * - 分页 {items, total, page, size}（0.4）
 * - 金额/税率/费率均为字符串（0.5，禁止浮点）
 * 后端 DTO：backend/src/main/java/com/hardmetal/export/**\/dto/*.java
 */

export interface ApiEnvelope<T> {
  code: number
  msg: string
  data: T
  traceId: string
}

export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

/* ----------------------------- 认证 ----------------------------- */

export interface LoginPayload {
  username: string
  password: string
}

export interface UserProfile {
  id: number
  username: string
  realName: string | null
  roleCode: string
  roleName: string
  department: string | null
  phone: string | null
  email: string | null
  lastLoginAt: string | null
  perms: string[]
}

export interface LoginResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: UserProfile
}

export interface RefreshResult {
  accessToken: string
  refreshToken: string
  expiresIn: number
}

/* ----------------------------- 字典 ----------------------------- */

export interface DictItem {
  value: string
  labelZh: string | null
  labelEn: string | null
  sort: number | null
}

export interface DictList {
  items: DictItem[]
}

/* ----------------------------- 用户 ----------------------------- */

/** GET /api/v1/users/options 的元素；仅用于归属员工下拉。 */
export interface UserOption {
  id: number
  username: string
  realName: string | null
  roleCode: string
}

export interface UserOptionsResult {
  items: UserOption[]
}

/* ----------------------------- 客户 ----------------------------- */

export interface CustomerView {
  id: number
  code: string
  nameCn: string | null
  nameEn: string | null
  customerType: string
  level: string
  countryCode: string
  machiningMaterials: string[]
  benchmarkBrand: string | null
  ownerId: number | null
  ownerName: string | null
  orderCount: number
  lastOrderAt: string | null
}

export interface ContactView {
  id: number
  name: string
  position: string | null
  email: string | null
  phone: string | null
  imType: string | null
  imAccount: string | null
  language: string | null
  isDecisionMaker: boolean
}

export interface PriceSummary {
  activeItems: number
  expiringSoon: number
}

export interface RecentQuote {
  id: number
  quoteNo: string
  status: string
  totalAmount: string
  currency: string
}

export interface CustomerDetailView {
  id: number
  code: string
  nameCn: string | null
  nameEn: string | null
  customerType: string
  level: string
  countryCode: string
  currency: string | null
  machiningMaterials: string[]
  machiningOps: string[]
  machineBrands: string[]
  benchmarkBrand: string | null
  paymentTerms: string | null
  creditDays: number | null
  creditLimit: string | null
  ownerId: number | null
  ownerName: string | null
  remark: string | null
  sanctionsStatus: string | null
  contacts: ContactView[]
  priceSummary: PriceSummary
  recentQuotes: RecentQuote[]
  orderCount: number
  lastOrderAt: string | null
}

export interface CreatedCustomer {
  id: number
  code: string
}

/** 新建/更新客户入参（CustomerUpsertRequest）。更新时只传要改的字段。 */
export interface CustomerUpsertPayload {
  code?: string
  nameCn?: string
  nameEn?: string
  customerType?: string
  level?: string
  countryCode?: string
  currency?: string
  machiningMaterials?: string[]
  machiningOps?: string[]
  machineBrands?: string[]
  benchmarkBrand?: string
  paymentTerms?: string
  creditDays?: number | null
  creditLimit?: string | null
  ownerId?: number | null
  remark?: string
}

/** 新增联系人入参（ContactUpsertRequest）。 */
export interface ContactPayload {
  name: string
  position?: string
  email?: string
  phone?: string
  imType?: string
  imAccount?: string
  language?: string
  isDecisionMaker?: boolean
}

export interface TimelineItem {
  at: string
  type: string
  title: string
  refId: number
  refType: string
  actor: string | null
}

/** 时间轴契约未定义 total（Controller 只返回 items）。 */
export interface TimelineResult {
  items: TimelineItem[]
}