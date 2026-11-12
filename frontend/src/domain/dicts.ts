import type { DictItem } from '../api/types'

/**
 * 字典兜底值。
 *
 * 运行期唯一来源是 GET /api/v1/meta/dict/{type}（《接口文档》12.1），
 * 这里只在接口不可用或首次加载完成前提供最小可读性，不参与提交校验。
 */
export const DICT_FALLBACKS: Record<string, DictItem[]> = {
  // 《接口文档》§0.5.1：不存在 oem；value 不可改，label 可随字典维护
  customer_type: [
    { value: 'distributor', labelZh: '经销商', labelEn: 'Distributor', sort: 10 },
    { value: 'end_user', labelZh: '终端工厂', labelEn: 'End User', sort: 20 },
    { value: 'trader', labelZh: '贸易商', labelEn: 'Trader', sort: 30 },
    { value: 'agent', labelZh: '代理商', labelEn: 'Agent', sort: 40 }
  ],
  customer_grade: [
    { value: 'A', labelZh: 'A 级', labelEn: null, sort: 10 },
    { value: 'B', labelZh: 'B 级', labelEn: null, sort: 20 },
    { value: 'C', labelZh: 'C 级', labelEn: null, sort: 30 },
    { value: 'D', labelZh: 'D 级', labelEn: null, sort: 40 }
  ],
  currency: [
    { value: 'USD', labelZh: 'USD', labelEn: null, sort: 10 },
    { value: 'EUR', labelZh: 'EUR', labelEn: null, sort: 20 },
    { value: 'CNY', labelZh: 'CNY', labelEn: null, sort: 30 },
    { value: 'RUB', labelZh: 'RUB', labelEn: null, sort: 40 },
    { value: 'INR', labelZh: 'INR', labelEn: null, sort: 50 },
    { value: 'GBP', labelZh: 'GBP', labelEn: null, sort: 60 },
    { value: 'JPY', labelZh: 'JPY', labelEn: null, sort: 70 }
  ]
}

/** 加工材料与工序：取值见《设计清单》C 段，后端按字符串数组存储，无字典接口。 */
export const MACHINING_MATERIALS = [
  'steel',
  'stainless',
  'cast_iron',
  'aluminum',
  'superalloy',
  'composite',
  'hardened_steel'
] as const

export const MACHINING_OPS = [
  'turning',
  'milling',
  'drilling',
  'boring',
  'threading',
  'parting',
  'grooving',
  'mold'
] as const

/** 客户类型标签，按《接口文档》§0.5.1 的顺序。 */
export const CUSTOMER_TYPE_ORDER = ['distributor', 'end_user', 'trader', 'agent'] as const