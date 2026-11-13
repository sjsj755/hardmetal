/**
 * 目标角色模型：只有 ADMIN / SALES 两级（见《设计清单》H 与
 * doc/内部自用精简-影响与后端清单.md）。
 *
 * 角色 code 与后端、接口保持一致（ADMIN / SALES）；界面按业务口径
 * 显示为“老板 / 员工”，仅做展示映射，不参与任何权限判断。
 */
export const ROLE_ADMIN = 'ADMIN'
export const ROLE_SALES = 'SALES'

export const WRITE_ROLE_CODES = [ROLE_ADMIN, ROLE_SALES] as const
export const DELETE_ROLE_CODES = [ROLE_ADMIN] as const

export const ROLE_LABELS: Record<string, string> = {
  [ROLE_ADMIN]: '老板',
  [ROLE_SALES]: '员工'
}

/** 未知角色回退显示原 code，绝不显示空白；仅用于界面展示。 */
export function roleLabel(code: string | null | undefined, fallback = ''): string {
  if (!code) return fallback
  return ROLE_LABELS[code] || fallback || code
}