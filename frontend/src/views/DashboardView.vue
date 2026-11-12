<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useDictStore } from '../stores/dict'
import { fetchCustomers } from '../api/customer'
import { errorMessage } from '../api/http'
import { CUSTOMER_TYPE_ORDER } from '../domain/dicts'
import { ROLE_ADMIN, ROLE_SALES } from '../domain/roles'
import { formatDateTime, formatPercent } from '../utils/format'
import PageHeader from '../components/PageHeader.vue'
import StatCard from '../components/StatCard.vue'

interface TypeCount {
  value: string
  label: string
  count: number
}

const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const loading = ref(true)
const errorText = ref('')
const total = ref(0)
const counts = ref<TypeCount[]>([])
const canCreate = computed(
  () => auth.hasPerm('customer:write') && auth.hasRole(ROLE_ADMIN, ROLE_SALES)
)

const dateText = new Date().toLocaleDateString('zh-CN', {
  year: 'numeric',
  month: 'long',
  day: 'numeric',
  weekday: 'long'
})

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) {
    return '夜深了'
  }
  if (hour < 12) {
    return '上午好'
  }
  if (hour < 14) {
    return '中午好'
  }
  if (hour < 18) {
    return '下午好'
  }
  return '晚上好'
})

const typeList = computed<TypeCount[]>(() =>
  counts.value.length
    ? counts.value
    : CUSTOMER_TYPE_ORDER.map((value) => ({
        value,
        label: dict.label('customer_type', value),
        count: 0
      }))
)

const maxCount = computed(() => Math.max(1, ...typeList.value.map((item) => item.count)))

const implementedApis = [
  { label: '登录 / 刷新 / 登出 / 当前用户', api: '/api/v1/auth/*' },
  { label: '客户列表与新建', api: 'GET / POST /api/v1/customers' },
  { label: '客户详情聚合', api: 'GET /api/v1/customers/{id}' },
  { label: '客户更新 / 删除', api: 'PUT / DELETE /api/v1/customers/{id}' },
  { label: '联系人维护', api: '/api/v1/customers/{id}/contacts' },
  { label: '客户时间轴', api: 'GET /api/v1/customers/{id}/timeline' },
  { label: '字典', api: 'GET /api/v1/meta/dict/{type}' }
]

const pendingApis = [
  { label: '工作台看板', api: 'GET /api/v1/dashboard' },
  { label: '产品 / 型号', api: '/api/v1/products' },
  { label: '价格表', api: '/api/v1/prices' },
  { label: '报价单', api: '/api/v1/quotations' },
  { label: '订单', api: '/api/v1/orders' },
  { label: '关税 / 运价', api: '/api/v1/tariffs · /freight' },
  { label: '合规筛查', api: '/api/v1/compliance' }
]

function barWidth(count: number): string {
  if (count <= 0) {
    return '0%'
  }
  return `${Math.max(6, (count / maxCount.value) * 100)}%`
}

async function load(): Promise<void> {
  loading.value = true
  errorText.value = ''
  try {
    await dict.load('customer_type').catch(() => null)
    const types = [...CUSTOMER_TYPE_ORDER]
    // 只调用已实现接口：total + 每类型各一次 size=1 的查询
    const [summary, ...byType] = await Promise.all([
      fetchCustomers({ page: 1, size: 1 }),
      ...types.map((type) => fetchCustomers({ page: 1, size: 1, type }))
    ])
    total.value = summary.total
    counts.value = types.map((value, index) => ({
      value,
      label: dict.label('customer_type', value),
      count: byType[index]?.total ?? 0
    }))
  } catch (error) {
    errorText.value = errorMessage(error)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="page">
    <PageHeader
      title="工作台"
      :subtitle="`${greeting}，${auth.displayName} · ${dateText}`"
    >
      <template #actions>
        <el-button :loading="loading" @click="load">刷新数据</el-button>
        <el-button type="primary" @click="router.push('/customers')">进入客户列表</el-button>
      </template>
    </PageHeader>

    <el-alert
      v-if="errorText"
      :title="errorText"
      type="error"
      :closable="false"
      show-icon
    >
      <template #default>
        <el-button link type="primary" @click="load">重试</el-button>
      </template>
    </el-alert>

    <div class="stat-grid" v-loading="loading">
      <StatCard
        label="客户总数（当前账号可见）"
        :value="total"
        tone="primary"
        hint="数据来自客户列表接口的 total"
      />
      <StatCard
        v-for="item in typeList"
        :key="item.value"
        :label="item.label"
        :value="item.count"
        :hint="`占比 ${formatPercent(item.count, total)}`"
      />
    </div>

    <div class="dash-grid">
      <section class="card card-pad">
        <div class="section-title">
          <h3>客户构成</h3>
          <span class="subtle">共 {{ total }} 位客户</span>
        </div>

        <div v-if="total > 0" class="bars">
          <div v-for="item in typeList" :key="item.value" class="bar-row">
            <span class="bar-label">{{ item.label }}</span>
            <div class="bar-track">
              <div class="bar-fill" :style="{ width: barWidth(item.count) }"></div>
            </div>
            <span class="bar-value">{{ item.count }}</span>
            <span class="bar-percent">{{ formatPercent(item.count, total) }}</span>
          </div>
        </div>

        <div v-else class="bars-empty">
          <p class="subtle">暂无客户数据。</p>
          <el-button
            v-if="canCreate"
            size="small"
            type="primary"
            plain
            @click="router.push('/customers')"
          >
            去新建客户
          </el-button>
        </div>
      </section>

      <section class="card card-pad side-card">
        <div class="section-title">
          <h3>关于本页</h3>
        </div>
        <p class="note">
          本页只展示已实现接口能提供的数据：客户总数、客户类型分布、当前账号与权限。
        </p>
        <p class="note">
          <span class="mono">GET /api/v1/dashboard</span> 在《接口文档》11.4 中已定义，后端尚未实现，
          因此待办、审批、钨价、汇率等指标暂不展示。
        </p>
        <el-divider />
        <div class="quick-links">
          <button class="quick-link" type="button" @click="router.push('/customers')">
            <span>
              <strong>客户管理</strong>
              <em>筛选、查看与维护客户档案</em>
            </span>
            <span class="quick-arrow">→</span>
          </button>
          <button class="quick-link" type="button" @click="load">
            <span>
              <strong>刷新统计</strong>
              <em>重新拉取客户总数与类型分布</em>
            </span>
            <span class="quick-arrow">→</span>
          </button>
        </div>
      </section>
    </div>

    <div class="dash-grid">
      <section class="card card-pad">
        <div class="section-title">
          <h3>接口接入状态</h3>
          <span class="subtle">前端不调用未实现接口</span>
        </div>
        <div class="api-columns">
          <div>
            <p class="api-heading api-heading--on">已接入</p>
            <ul class="api-list">
              <li v-for="item in implementedApis" :key="item.api">
                <span class="dot dot--on"></span>
                <span class="api-label">{{ item.label }}</span>
                <span class="api-path mono">{{ item.api }}</span>
              </li>
            </ul>
          </div>
          <div>
            <p class="api-heading api-heading--off">待后端实现</p>
            <ul class="api-list">
              <li v-for="item in pendingApis" :key="item.api">
                <span class="dot"></span>
                <span class="api-label">{{ item.label }}</span>
                <span class="api-path mono">{{ item.api }}</span>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section class="card card-pad">
        <div class="section-title">
          <h3>账号与权限</h3>
          <el-tag size="small" type="info" effect="plain">{{ auth.user?.roleName || '—' }}</el-tag>
        </div>
        <dl class="account-list">
          <div>
            <dt>登录名</dt>
            <dd class="mono">{{ auth.user?.username || '—' }}</dd>
          </div>
          <div>
            <dt>姓名</dt>
            <dd>{{ auth.user?.realName || '—' }}</dd>
          </div>
          <div>
            <dt>角色代码</dt>
            <dd class="mono">{{ auth.user?.roleCode || '—' }}</dd>
          </div>
          <div>
            <dt>部门</dt>
            <dd>{{ auth.user?.department || '—' }}</dd>
          </div>
          <div>
            <dt>最近登录</dt>
            <dd>{{ formatDateTime(auth.user?.lastLoginAt) }}</dd>
          </div>
        </dl>
        <p class="perm-label">权限点（{{ auth.perms.length }}）</p>
        <div class="chip-row">
          <el-tag
            v-for="perm in auth.perms"
            :key="perm"
            size="small"
            effect="plain"
            type="info"
          >
            {{ perm }}
          </el-tag>
          <span v-if="auth.perms.length === 0" class="subtle">—</span>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped>
.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 14px;
}

.dash-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) minmax(0, 0.7fr);
  gap: 16px;
}

.bars {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding-top: 4px;
}

.bar-row {
  display: grid;
  grid-template-columns: 84px minmax(0, 1fr) 42px 58px;
  align-items: center;
  gap: 12px;
}

.bar-label {
  color: var(--text-2);
  font-size: 13px;
}

.bar-track {
  height: 8px;
  border-radius: 999px;
  background: var(--surface-2);
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #a9c0f8, #5b84ea);
  transition: width 0.3s ease;
}

.bar-value {
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.bar-percent {
  color: var(--text-3);
  font-size: 12px;
  text-align: right;
}

.bars-empty {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 0 6px;
}

.side-card {
  display: flex;
  flex-direction: column;
}

.note {
  color: var(--text-2);
  font-size: 12.5px;
  line-height: 1.85;
  margin-bottom: 8px;
}

.quick-links {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.quick-link {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--surface);
  text-align: left;
  cursor: pointer;
  font-family: inherit;
  transition:
    border-color 0.15s ease,
    background-color 0.15s ease;
}

.quick-link:hover {
  border-color: #c7d6f6;
  background: #f8fbff;
}

.quick-link strong {
  display: block;
  color: var(--text);
  font-size: 13px;
  font-weight: 600;
}

.quick-link em {
  display: block;
  margin-top: 2px;
  color: var(--text-3);
  font-size: 11.5px;
  font-style: normal;
}

.quick-arrow {
  color: var(--primary);
  font-size: 15px;
}

.api-columns {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 20px;
}

.api-heading {
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.03em;
  margin-bottom: 8px;
}

.api-heading--on {
  color: var(--success);
}

.api-heading--off {
  color: var(--text-3);
}

.api-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.api-list li {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 12px;
}

.dot {
  flex: none;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #d5dde8;
  transform: translateY(-1px);
}

.dot--on {
  background: var(--success);
}

.api-label {
  color: var(--text);
  white-space: nowrap;
}

.api-path {
  color: var(--text-3);
  font-size: 10.5px;
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin: 0 0 16px;
}

.account-list div {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.account-list dt {
  color: var(--text-3);
  font-size: 12px;
}

.account-list dd {
  margin: 0;
  color: var(--text);
  font-size: 12.5px;
}

.perm-label {
  color: var(--text-3);
  font-size: 12px;
  margin-bottom: 8px;
}

@media (max-width: 1000px) {
  .dash-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .api-columns {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 560px) {
  .bar-row {
    grid-template-columns: 72px minmax(0, 1fr) 34px;
  }

  .bar-percent {
    display: none;
  }
}
</style>