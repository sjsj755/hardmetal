<script setup lang="ts">
import { ArrowLeft } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const props = withDefaults(
  defineProps<{
    title: string
    subtitle?: string
    backTo?: string
  }>(),
  { subtitle: '', backTo: '' }
)

const router = useRouter()

function goBack(): void {
  router.push(props.backTo || '/dashboard')
}
</script>

<template>
  <div class="page-header">
    <div class="page-header-main">
      <el-button
        v-if="backTo"
        class="back-btn"
        text
        :icon="ArrowLeft"
        aria-label="返回"
        @click="goBack"
      />
      <div>
        <h1 class="page-title">{{ title }}</h1>
        <p v-if="subtitle" class="page-subtitle">{{ subtitle }}</p>
      </div>
    </div>
    <div v-if="$slots.actions" class="page-header-actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.page-header-main {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.back-btn {
  margin-right: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  letter-spacing: 0.01em;
  line-height: 1.3;
}

.page-subtitle {
  margin-top: 2px;
  color: var(--text-2);
  font-size: 13px;
}

.page-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>