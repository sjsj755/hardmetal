import { request } from './http'
import type { DictList } from './types'

/** GET /api/v1/meta/dict/{type}（《接口文档》12.1）：字典类型不存在返回 40401。 */
export function fetchDict(type: string): Promise<DictList> {
  return request<DictList>({
    url: `/meta/dict/${encodeURIComponent(type)}`,
    method: 'get'
  })
}