<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="card card-pad">
      <p class="code">404</p>
      <h1>页面不存在</h1>
      <p class="muted">地址可能已变更，或该模块尚未开放。</p>
      <el-button type="primary" @click="router.replace('/dashboard')">返回工作台</el-button>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  display: grid;
  place-items: center;
  min-height: 100vh;
  padding: 24px;
}

.card {
  width: min(420px, 100%);
  padding: 40px;
  text-align: center;
}

.code {
  color: var(--primary);
  font-family: var(--mono);
  font-size: 44px;
  font-weight: 700;
  line-height: 1;
}

h1 {
  margin: 10px 0 6px;
  font-size: 18px;
  font-weight: 600;
}

.muted {
  margin-bottom: 20px;
}
</style>