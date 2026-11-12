/**
 * 展示层格式化。金额一律按字符串显示（《接口文档》0.5：金额/税率/费率均为字符串），
 * 任何情况下都不把金额转成 Number，避免浮点误差。
 */

export function formatDate(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const dateOnly = /^\d{4}-\d{2}-\d{2}/.exec(value)
  return dateOnly ? dateOnly[0] : value
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return '—'
  }
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) {
    return value
  }
  return parsed.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  })
}

export function formatAmount(value?: string | null, currency?: string | null): string {
  if (value === null || value === undefined || value === '') {
    return '—'
  }
  return currency ? `${currency} ${value}` : value
}

export function formatDays(value?: number | null): string {
  return value === null || value === undefined ? '—' : `${value} 天`
}

export function initials(name?: string | null): string {
  const text = (name ?? '').trim()
  if (!text) {
    return '?'
  }
  if (/^[A-Za-z]/.test(text)) {
    return text
      .split(/\s+/)
      .slice(0, 2)
      .map((part) => part.charAt(0).toUpperCase())
      .join('')
  }
  return text.slice(0, 2)
}

export function formatPercent(value: number, total: number): string {
  if (!total) {
    return '0%'
  }
  return `${((value / total) * 100).toFixed(1)}%`
}