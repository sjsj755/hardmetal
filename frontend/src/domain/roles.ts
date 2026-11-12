/**
 * 目标角色模型：只有 ADMIN / SALES 两级（见《设计清单》H 与
 * doc/内部自用精简-影响与后端清单.md）。
 *
 * 后端 V2 仍是 6 角色，迁移完成前前端按目标模型门禁：旧角色
 * （BOSS / SALES_MANAGER / DOC_SPECIALIST / FINANCE）不再放行
 * 客户写操作，避免出现“按钮可见、提交 40301”的死按钮。
 */
export const ROLE_ADMIN = 'ADMIN'
export const ROLE_SALES = 'SALES'

export const WRITE_ROLE_CODES = [ROLE_ADMIN, ROLE_SALES] as const
export const DELETE_ROLE_CODES = [ROLE_ADMIN] as const

export const ROLE_LABELS: Record<string, string> = {
  [ROLE_ADMIN]: '管理员',
  [ROLE_SALES]: '业务员'
}

export function roleLabel(code: string | null | undefined, fallback = ''): string {
  if (!code) return fallback
  return ROLE_LABELS[code] || fallback || code
}