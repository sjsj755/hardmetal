import { ref } from 'vue'
import { defineStore } from 'pinia'
import { fetchDict } from '../api/meta'
import type { DictItem } from '../api/types'
import { DICT_FALLBACKS } from '../domain/dicts'

/**
 * 字典缓存：同一 type 只请求一次；并发调用共用 in-flight promise。
 * 数据源 GET /api/v1/meta/dict/{type}（《接口文档》12.1）。
 */
export const useDictStore = defineStore('dict', () => {
  const buckets = ref<Record<string, DictItem[]>>({})
  const loading = ref<Record<string, boolean>>({})

  const inflight = new Map<string, Promise<DictItem[]>>()

  function items(type: string): DictItem[] {
    return buckets.value[type] ?? DICT_FALLBACKS[type] ?? []
  }

  function label(type: string, value?: string | null): string {
    if (!value) {
      return ''
    }
    const hit = items(type).find((item) => item.value === value)
    return hit?.labelZh || hit?.labelEn || value
  }

  function isLoaded(type: string): boolean {
    return Boolean(buckets.value[type])
  }

  async function load(type: string, force = false): Promise<DictItem[]> {
    const cached = buckets.value[type]
    if (!force && cached) {
      return cached
    }
    const running = inflight.get(type)
    if (running) {
      return running
    }
    loading.value = { ...loading.value, [type]: true }
    const task = fetchDict(type)
      .then((response) => {
        buckets.value = { ...buckets.value, [type]: response.items }
        return response.items
      })
      .finally(() => {
        inflight.delete(type)
        loading.value = { ...loading.value, [type]: false }
      })
    inflight.set(type, task)
    return task
  }

  return { buckets, loading, items, label, isLoaded, load }
})