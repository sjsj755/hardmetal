<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import DictLabel from '../components/DictLabel.vue'
import CustomerFormDialog from '../components/CustomerFormDialog.vue'
import { fetchCustomers } from '../api/customer'
import { errorMessage } from '../api/http'
import { ROLE_ADMIN, ROLE_SALES } from '../domain/roles'
import { useAuthStore } from '../stores/auth'
import { useDictStore } from '../stores/dict'
import { formatDate } from '../utils/format'
import type { CreatedCustomer, CustomerView } from '../api/types'

const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const filters = reactive({
  keyword: '',
  type: '',
  level: '',
  country: ''
})

const items = ref<CustomerView[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const loading = ref(false)
const errorText = ref('')
const createVisible = ref(false)

// 目标角色模型只有 ADMIN / SALES 两级；后端迁移完成前，旧角色
// （BOSS/SALES_MANAGER/DOC_SPECIALIST/FINANCE）不放行，避免死按钮。
const canCreate = computed(
  () => auth.hasPerm('customer:write') && auth.hasRole(ROLE_ADMIN, ROLE_SALES)
)
const customerTypes = computed(() => dict.items('customer_type'))
const grades = computed(() => dict.items('customer_grade'))
const subtitle = computed(() => (loading.value ? '加载中…' : `共 ${total.value} 位客户`))

async function fetchList(): Promise<void> {
  loading.value = true
  errorText.value = ''
  try {
    const result = await fetchCustomers({
      page: page.value,
      size: size.value,
      type: filters.type || undefined,
      level: filters.level || undefined,
      country: filters.country.trim() || undefined,
      keyword: filters.keyword.trim() || undefined
    })
    items.value = result.items
    total.value = result.total
    // 删除/筛选后当前页可能为空，自动回到第一页
    if (items.value.length === 0 && total.value > 0 && page.value > 1) {
      page.value = 1
      await fetchList()
    }
  } catch (error) {
    errorText.value = errorMessage(error)
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all(
    ['customer_type', 'customer_grade'].map((type) => dict.load(type).catch(() => null))
  )
  await fetchList()
})

function search(): void {
  page.value = 1
  void fetchList()
}

function reset(): void {
  filters.keyword = ''
  filters.type = ''
  filters.level = ''
  filters.country = ''
  page.value = 1
  void fetchList()
}

function onPageChange(next: number): void {
  page.value = next
  void fetchList()
}

function onSizeChange(next: number): void {
  size.value = next
  page.value = 1
  void fetchList()
}

function openDetail(row: CustomerView): void {
  void router.push({ name: 'customer-detail', params: { id: row.id } })
}

// el-table 的默认插槽把 row 推断为 DefaultRow（Record<string, any>），
// 这里只在模板边界做一次收窄，业务函数仍保持强类型。
function openDetailFromRow(row: unknown): void {
  openDetail(row as CustomerView)
}

function handleCreated(created?: CreatedCustomer): void {
  if (created) {
    ElMessage.success(`客户 ${created.code} 已创建`)
  }
  page.value = 1
  void fetchList()
}
</script>

<template>
  <div class="page">
    <PageHeader title="客户列表" :subtitle="subtitle">
      <template #actions>
        <el-button :loading="loading" @click="fetchList">刷新</el-button>
        <el-button v-if="canCreate" type="primary" @click="createVisible = true">
          新建客户
        </el-button>
      </template>
    </PageHeader>

    <section class="card filter-card">
      <div class="filters">
        <el-input
          v-model="filters.keyword"
          class="filter-keyword"
          clearable
          :prefix-icon="Search"
          placeholder="搜索客户编码 / 中文名 / 英文名"
          @keyup.enter="search"
        />
        <el-select v-model="filters.type" class="filter-select" clearable placeholder="客户类型">
          <el-option
            v-for="item in customerTypes"
            :key="item.value"
            :label="item.labelZh || item.value"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filters.level" class="filter-select" clearable placeholder="客户等级">
          <el-option
            v-for="item in grades"
            :key="item.value"
            :label="item.labelZh || item.value"
            :value="item.value"
          />
        </el-select>
        <el-input
          v-model="filters.country"
          class="filter-country mono"
          maxlength="2"
          clearable
          placeholder="国家，如 DE"
          @keyup.enter="search"
        />
        <el-button type="primary" plain @click="search">查询</el-button>
        <el-button @click="reset">重置</el-button>
      </div>
    </section>

    <el-alert
      v-if="errorText"
      :title="errorText"
      type="error"
      :closable="false"
      show-icon
    />

    <section class="card table-card">
      <el-table
        :data="items"
        v-loading="loading"
        row-class-name="clickable-row"
        @row-click="openDetail"
      >
        <el-table-column label="编码" width="120">
          <template #default="{ row }">
            <span class="mono">{{ row.code }}</span>
          </template>
        </el-table-column>

        <el-table-column label="客户名称" min-width="230">
          <template #default="{ row }">
            <div class="customer-cell">
              <span class="customer-name">{{ row.nameCn || row.nameEn || '—' }}</span>
              <span v-if="row.nameCn && row.nameEn" class="customer-en">{{ row.nameEn }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="类型" width="104">
          <template #default="{ row }">
            <DictLabel type="customer_type" :value="row.customerType" tone="type" />
          </template>
        </el-table-column>

        <el-table-column label="等级" width="84">
          <template #default="{ row }">
            <DictLabel type="customer_grade" :value="row.level" tone="level" />
          </template>
        </el-table-column>

        <el-table-column label="国家" width="76">
          <template #default="{ row }">
            <span class="mono">{{ row.countryCode }}</span>
          </template>
        </el-table-column>

        <el-table-column label="加工材料" min-width="190">
          <template #default="{ row }">
            <div class="chip-row">
              <el-tag
                v-for="material in row.machiningMaterials.slice(0, 3)"
                :key="material"
                size="small"
                effect="plain"
              >
                {{ material }}
              </el-tag>
              <span v-if="row.machiningMaterials.length > 3" class="more">
                +{{ row.machiningMaterials.length - 3 }}
              </span>
              <span v-if="row.machiningMaterials.length === 0" class="subtle">—</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="订单" width="76" align="right">
          <template #default="{ row }">
            <span class="mono">{{ row.orderCount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="最近下单" width="110">
          <template #default="{ row }">
            <span class="subtle">{{ formatDate(row.lastOrderAt) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click.stop="openDetailFromRow(row)">查看</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <EmptyState
            :title="errorText ? '数据加载失败' : '没有符合条件的客户'"
            :description="
              errorText ? errorText : '调整筛选条件，或新建一条客户档案。'
            "
          >
            <el-button
              v-if="!errorText && canCreate"
              type="primary"
              @click="createVisible = true"
            >
              新建客户
            </el-button>
          </EmptyState>
        </template>
      </el-table>

      <div v-if="total > 0" class="pager">
        <el-pagination
          :current-page="page"
          :page-size="size"
          :page-sizes="[20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next"
          background
          @current-change="onPageChange"
          @size-change="onSizeChange"
        />
      </div>
    </section>

    <CustomerFormDialog v-model:visible="createVisible" mode="create" @saved="handleCreated" />
  </div>
</template>

<style scoped>
.filter-card {
  padding: 16px 18px;
}

.filters {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.filter-keyword {
  width: 280px;
}

.filter-select {
  width: 148px;
}

.filter-country {
  width: 132px;
}

.customer-cell {
  display: flex;
  flex-direction: column;
  line-height: 1.35;
}

.customer-name {
  color: var(--text);
  font-weight: 500;
}

.customer-en {
  color: var(--text-3);
  font-size: 11.5px;
}

.more {
  color: var(--text-3);
  font-size: 11.5px;
  align-self: center;
}

.table-card {
  padding-bottom: 4px;
  overflow: hidden;
}

.table-card :deep(.el-table) {
  --el-table-border-color: transparent;
}

.table-card :deep(.el-table__inner-wrapper::before) {
  display: none;
}

.pager {
  padding: 14px 16px 12px;
}

@media (max-width: 760px) {
  .filter-keyword,
  .filter-select,
  .filter-country {
    width: 100%;
  }
}
</style>