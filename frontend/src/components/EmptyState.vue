<script setup lang="ts">
withDefaults(
  defineProps<{
    title: string
    description?: string
  }>(),
  { description: '' }
)
</script>

<template>
  <div class="empty-state">
    <div class="empty-mark" aria-hidden="true">
      <span></span>
      <span></span>
      <span></span>
    </div>
    <p class="empty-title">{{ title }}</p>
    <p v-if="description" class="empty-desc">{{ description }}</p>
    <div v-if="$slots.default" class="empty-actions">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 36px 20px;
  text-align: center;
}

.empty-mark {
  display: flex;
  align-items: flex-end;
  gap: 4px;
  width: 44px;
  height: 36px;
  padding: 10px;
  margin-bottom: 6px;
  border-radius: 12px;
  background: var(--surface-2);
}

.empty-mark span {
  flex: 1;
  border-radius: 3px;
  background: #cdd9ec;
}

.empty-mark span:nth-child(1) {
  height: 40%;
}

.empty-mark span:nth-child(2) {
  height: 85%;
}

.empty-mark span:nth-child(3) {
  height: 60%;
}

.empty-title {
  font-weight: 600;
  font-size: 14px;
}

.empty-desc {
  max-width: 420px;
  color: var(--text-2);
  font-size: 13px;
  line-height: 1.7;
}

.empty-actions {
  margin-top: 10px;
}
</style>