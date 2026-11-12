<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    tone?: 'default' | 'primary' | 'success' | 'warning'
  }>(),
  { hint: '', tone: 'default' }
)
</script>

<template>
  <div class="stat-card card" :class="`tone-${tone}`">
    <p class="stat-label">{{ label }}</p>
    <p class="stat-value">{{ value }}</p>
    <p v-if="hint" class="stat-hint">{{ hint }}</p>
  </div>
</template>

<style scoped>
.stat-card {
  padding: 18px 20px;
  border-top: 2px solid var(--border);
}

.tone-primary {
  border-top-color: #9db8f2;
}

.tone-success {
  border-top-color: #9fd8c3;
}

.tone-warning {
  border-top-color: #eed3a0;
}

.stat-label {
  color: var(--text-2);
  font-size: 12.5px;
  letter-spacing: 0.02em;
}

.stat-value {
  margin-top: 6px;
  font-size: 28px;
  font-weight: 600;
  line-height: 1.2;
  letter-spacing: -0.01em;
}

.stat-hint {
  margin-top: 6px;
  color: var(--text-3);
  font-size: 12px;
}
</style>