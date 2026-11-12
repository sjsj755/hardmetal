<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useDictStore } from '../stores/dict'

type TagType = 'primary' | 'success' | 'info' | 'warning' | 'danger'

const props = withDefaults(
  defineProps<{
    type: string
    value?: string | null
    tone?: 'plain' | 'type' | 'level' | 'status' | 'sanction'
    size?: 'small' | 'default' | 'large'
  }>(),
  { value: null, tone: 'plain', size: 'small' }
)

const dict = useDictStore()

onMounted(() => {
  dict.load(props.type).catch(() => {
    // 字典不可用时 DictLabel 回落到原始 value，不阻塞页面
  })
})

const label = computed(() => dict.label(props.type, props.value))
const tagType = computed<TagType>(() => {
  switch (props.tone) {
    case 'type':
      return 'primary'
    case 'level':
      return levelType(props.value)
    case 'status':
      return statusType(props.value)
    case 'sanction':
      return sanctionType(props.value)
    default:
      return 'info'
  }
})

function levelType(value?: string | null): TagType {
  switch (value) {
    case 'A':
      return 'success'
    case 'B':
      return 'primary'
    case 'D':
      return 'warning'
    default:
      return 'info'
  }
}

function statusType(value?: string | null): TagType {
  switch (value) {
    case 'accepted':
    case 'converted':
      return 'success'
    case 'rejected':
      return 'danger'
    case 'expired':
    case 'internal_review':
      return 'warning'
    case 'sent':
      return 'primary'
    default:
      return 'info'
  }
}

function sanctionType(value?: string | null): TagType {
  switch (value) {
    case 'clear':
      return 'success'
    case 'review':
      return 'warning'
    case 'blocked':
      return 'danger'
    default:
      return 'info'
  }
}
</script>

<template>
  <el-tag v-if="value" :type="tagType" :size="size" effect="light" round>
    {{ label }}
  </el-tag>
  <span v-else class="subtle">—</span>
</template>