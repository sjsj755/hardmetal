<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import PageHeader from '../components/PageHeader.vue'
import EmptyState from '../components/EmptyState.vue'
import DictLabel from '../components/DictLabel.vue'
import CustomerFormDialog from '../components/CustomerFormDialog.vue'
import ContactFormDialog from '../components/ContactFormDialog.vue'
import { deleteContact, deleteCustomer, fetchCustomer, fetchTimeline } from '../api/customer'
import { ApiError, errorMessage } from '../api/http'
import { ROLE_ADMIN, ROLE_SALES } from '../domain/roles'
import { useAuthStore } from '../stores/auth'
import { useDictStore } from '../stores/dict'
import {
  formatAmount,
  formatDate,
  formatDateTime,
  formatDays,
  initials
} from '../utils/format'
import type { ContactView, CustomerDetailView, TimelineItem } from '../api/types'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const dict = useDictStore()

const customerId = computed(() => Number(route.params.id))
const detail = ref<CustomerDetailView | null>(null)
const loading = ref(true)
const loadError = ref<{ code: number; message: string } | null>(null)

const editVisible = ref(false)
const contactVisible = ref(false)

const TIMELINE_PAGE_SIZE = 50
const timelineItems = ref<TimelineItem[]>([])
const timelinePage = ref(1)
const timelineType = ref('')
const timelineLoading = ref(false)
const timelineHasMore = ref(false)
const timelineError = ref('')

// 目标角色模型：ADMIN / SALES 可写；删除仅 ADMIN（后端迁移前同样按目标模型门禁）。
const canWrite = computed(
  () => auth.hasPerm('customer:write') && auth.hasRole(ROLE_ADMIN, ROLE_SALES)
)
const canDelete = computed(() => auth.hasRole(ROLE_ADMIN))

const customerName = computed(
  () => detail.value?.nameCn || detail.value?.nameEn || detail.value?.code || '客户详情'
)
const subtitle = computed(() =>
  detail.value ? `${detail.value.code} · ${detail.value.countryCode}` : ''
)
const errorTitle = computed(() => {
  const code = loadError.value?.code
  if (code === 40301) {
    return '无权查看该客户'
  }
  if (code === 40401) {
    return '客户不存在或已删除'
  }
  return '客户详情加载失败'
})

let timelineRequestId = 0

async function loadTimeline(reset: boolean): Promise<void> {
  if (!Number.isFinite(customerId.value)) {
    return
  }
  if (!reset && timelineLoading.value) {
    return
  }
  if (reset) {
    timelinePage.value = 1
    timelineItems.value = []
    timelineHasMore.value = false
  }
  const requestId = ++timelineRequestId
  timelineLoading.value = true
  timelineError.value = ''
  try {
    const result = await fetchTimeline(customerId.value, {
      type: timelineType.value || undefined,
      page: timelinePage.value
    })
    if (requestId !== timelineRequestId) {
      return
    }
    timelineItems.value = reset
      ? result.items
      : [...timelineItems.value, ...result.items]
    timelineHasMore.value = result.items.length >= TIMELINE_PAGE_SIZE
  } catch (error) {
    if (requestId === timelineRequestId) {
      timelineError.value = errorMessage(error)
    }
  } finally {
    if (requestId === timelineRequestId) {
      timelineLoading.value = false
    }
  }
}

async function load(): Promise<void> {
  if (!Number.isFinite(customerId.value)) {
    loadError.value = { code: 40401, message: '客户编号无效' }
    loading.value = false
    return
  }
  loading.value = true
  loadError.value = null
  detail.value = null
  try {
    detail.value = await fetchCustomer(customerId.value)
    await Promise.all([
      dict.load('customer_type').catch(() => null),
      dict.load('customer_grade').catch(() => null),
      dict.load('quote_status').catch(() => null)
    ])
    await loadTimeline(true)
  } catch (error) {
    detail.value = null
    loadError.value =
      error instanceof ApiError
        ? { code: error.code, message: error.message }
        : { code: -1, message: '加载失败，请稍后重试' }
  } finally {
    loading.value = false
  }
}

async function refreshDetail(): Promise<void> {
  if (!Number.isFinite(customerId.value)) {
    return
  }
  try {
    detail.value = await fetchCustomer(customerId.value)
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function changeTimelineType(): void {
  void loadTimeline(true)
}

function loadMoreTimeline(): void {
  if (timelineLoading.value || !timelineHasMore.value) {
    return
  }
  timelinePage.value += 1
  void loadTimeline(false)
}

function timelineTypeLabel(type: string): string {
  if (type === 'quote') {
    return '报价'
  }
  if (type === 'order') {
    return '订单'
  }
  return type
}

function sanctionLabel(value?: string | null): string {
  if (!value) {
    return '未知'
  }
  const labels: Record<string, string> = {
    clear: '正常',
    review: '待复核',
    blocked: '已阻断'
  }
  return labels[value] ?? value
}

function sanctionTagType(value?: string | null): 'success' | 'warning' | 'danger' | 'info' {
  if (value === 'clear') {
    return 'success'
  }
  if (value === 'review') {
    return 'warning'
  }
  if (value === 'blocked') {
    return 'danger'
  }
  return 'info'
}

async function handleDelete(): Promise<void> {
  const current = detail.value
  if (!current) {
    return
  }
  const name = current.nameCn || current.code
  try {
    await ElMessageBox.confirm(
      `删除客户「${name}」后不可恢复。若该客户存在未结报价或订单，后端会拒绝删除。`,
      '删除客户',
      {
        type: 'warning',
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger'
      }
    )
  } catch {
    return
  }
  try {
    await deleteCustomer(current.id)
    ElMessage.success('客户已删除')
    await router.replace({ name: 'customers' })
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

async function removeContact(contact: ContactView): Promise<void> {
  const current = detail.value
  if (!current) {
    return
  }
  try {
    await ElMessageBox.confirm(`确定删除联系人「${contact.name}」？`, '删除联系人', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return
  }
  try {
    await deleteContact(current.id, contact.id)
    ElMessage.success('联系人已删除')
    await refreshDetail()
  } catch (error) {
    ElMessage.error(errorMessage(error))
  }
}

function handleEdited(): void {
  ElMessage.success('客户信息已更新')
  void refreshDetail()
}

function handleContactSaved(contact: ContactView): void {
  ElMessage.success(`联系人 ${contact.name} 已添加`)
  void refreshDetail()
}

onMounted(() => {
  void load()
})

watch(customerId, () => {
  void load()
})
</script>

<template>
  <div class="page">
    <PageHeader :title="customerName" :subtitle="subtitle" back-to="/customers">
      <template #actions>
        <el-button :disabled="!detail" @click="refreshDetail">刷新</el-button>
        <el-button v-if="detail && canWrite" @click="editVisible = true">编辑</el-button>
        <el-button v-if="detail && canDelete" type="danger" plain @click="handleDelete">
          删除
        </el-button>
      </template>
    </PageHeader>

    <div v-if="loading" class="card card-pad">
      <el-skeleton :rows="6" animated />
    </div>

    <section v-else-if="loadError" class="card card-pad">
      <EmptyState :title="errorTitle" :description="loadError.message">
        <el-button @click="router.push('/customers')">返回客户列表</el-button>
        <el-button
          v-if="loadError.code !== 40301 && loadError.code !== 40401"
          type="primary"
          @click="load"
        >
          重试
        </el-button>
      </EmptyState>
    </section>

    <template v-else-if="detail">
      <section class="hero card card-pad">
        <div class="hero-main">
          <span class="hero-avatar">{{ initials(detail.nameCn || detail.nameEn) }}</span>
          <div class="hero-text">
            <h2 class="hero-name">{{ detail.nameCn || detail.nameEn || '未命名客户' }}</h2>
            <p v-if="detail.nameEn && detail.nameCn" class="hero-en">{{ detail.nameEn }}</p>
            <div class="chip-row hero-chips">
              <DictLabel type="customer_type" :value="detail.customerType" tone="type" />
              <DictLabel type="customer_grade" :value="detail.level" tone="level" />
              <el-tag size="small" type="info" effect="plain">{{ detail.countryCode }}</el-tag>
              <el-tag
                size="small"
                :type="sanctionTagType(detail.sanctionsStatus)"
                effect="light"
                round
              >
                制裁：{{ sanctionLabel(detail.sanctionsStatus) }}
              </el-tag>
            </div>
          </div>
        </div>

        <div class="hero-metrics">
          <div class="hero-metric">
            <span class="hero-metric-value mono">{{ detail.orderCount }}</span>
            <span class="hero-metric-label">累计订单</span>
          </div>
          <div class="hero-metric">
            <span class="hero-metric-value mono">{{ formatDate(detail.lastOrderAt) }}</span>
            <span class="hero-metric-label">最近下单</span>
          </div>
          <div class="hero-metric">
            <span class="hero-metric-value mono">
              {{ formatAmount(detail.creditLimit, detail.currency) }}
            </span>
            <span class="hero-metric-label">授信额度</span>
          </div>
        </div>
      </section>

      <div class="detail-grid">
        <div class="detail-column">
          <section class="card card-pad">
            <div class="section-title">
              <h3>客户档案</h3>
              <span class="subtle">数据来自客户详情聚合接口</span>
            </div>

            <el-descriptions :column="2" class="desc">
              <el-descriptions-item label="客户编码">
                <span class="mono">{{ detail.code }}</span>
              </el-descriptions-item>
              <el-descriptions-item label="结算币种">
                <span class="mono">{{ detail.currency || '—' }}</span>
              </el-descriptions-item>

              <el-descriptions-item label="付款条款" :span="2">
                {{ detail.paymentTerms || '—' }}
              </el-descriptions-item>

              <el-descriptions-item label="账期">
                {{ formatDays(detail.creditDays) }}
              </el-descriptions-item>
              <el-descriptions-item label="授信额度">
                <span class="mono">{{ formatAmount(detail.creditLimit, detail.currency) }}</span>
              </el-descriptions-item>

              <el-descriptions-item label="对标品牌">
                {{ detail.benchmarkBrand || '—' }}
              </el-descriptions-item>
              <el-descriptions-item label="制裁状态">
                <el-tag
                  size="small"
                  :type="sanctionTagType(detail.sanctionsStatus)"
                  effect="light"
                  round
                >
                  {{ sanctionLabel(detail.sanctionsStatus) }}
                </el-tag>
              </el-descriptions-item>

              <el-descriptions-item label="加工材料" :span="2">
                <div v-if="detail.machiningMaterials.length" class="chip-row">
                  <el-tag
                    v-for="material in detail.machiningMaterials"
                    :key="material"
                    size="small"
                    effect="plain"
                  >
                    {{ material }}
                  </el-tag>
                </div>
                <span v-else class="subtle">—</span>
              </el-descriptions-item>

              <el-descriptions-item label="加工工序" :span="2">
                <div v-if="detail.machiningOps.length" class="chip-row">
                  <el-tag
                    v-for="op in detail.machiningOps"
                    :key="op"
                    size="small"
                    effect="plain"
                  >
                    {{ op }}
                  </el-tag>
                </div>
                <span v-else class="subtle">—</span>
              </el-descriptions-item>

              <el-descriptions-item label="机床品牌" :span="2">
                <div v-if="detail.machineBrands.length" class="chip-row">
                  <el-tag
                    v-for="brand in detail.machineBrands"
                    :key="brand"
                    size="small"
                    effect="plain"
                  >
                    {{ brand }}
                  </el-tag>
                </div>
                <span v-else class="subtle">—</span>
              </el-descriptions-item>
            </el-descriptions>
          </section>

          <section class="card card-pad">
            <div class="section-title">
              <h3>最近报价</h3>
              <span class="subtle">按客户聚合返回，最多 5 条</span>
            </div>

            <el-table v-if="detail.recentQuotes.length" :data="detail.recentQuotes" size="small">
              <el-table-column label="报价单号" min-width="160">
                <template #default="{ row }">
                  <span class="mono">{{ row.quoteNo }}</span>
                </template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{ row }">
                  <DictLabel type="quote_status" :value="row.status" tone="status" />
                </template>
              </el-table-column>
              <el-table-column label="金额" width="150" align="right">
                <template #default="{ row }">
                  <span class="mono">{{ formatAmount(row.totalAmount, row.currency) }}</span>
                </template>
              </el-table-column>
            </el-table>

            <EmptyState
              v-else
              title="暂无报价"
              description="报价模块接口尚未实现；这里只展示客户详情聚合中已有的数据。"
            />

            <p class="card-note">报价详情页将在报价模块上线后开放。</p>
          </section>

          <section class="card card-pad">
            <div class="section-title">
              <h3>时间轴</h3>
              <el-select
                v-model="timelineType"
                size="small"
                style="width: 120px"
                @change="changeTimelineType"
              >
                <el-option label="全部事件" value="" />
                <el-option label="报价" value="quote" />
                <el-option label="订单" value="order" />
              </el-select>
            </div>

            <el-alert
              v-if="timelineError"
              :title="timelineError"
              type="error"
              :closable="false"
              show-icon
              class="timeline-alert"
            />

            <div v-loading="timelineLoading" class="timeline-body">
              <el-timeline v-if="timelineItems.length">
                <el-timeline-item
                  v-for="item in timelineItems"
                  :key="`${item.refType}-${item.refId}-${item.at}`"
                  :timestamp="formatDateTime(item.at)"
                  placement="top"
                >
                  <div class="timeline-card">
                    <div class="timeline-head">
                      <el-tag
                        size="small"
                        :type="item.type === 'quote' ? 'primary' : 'success'"
                        effect="light"
                        round
                      >
                        {{ timelineTypeLabel(item.type) }}
                      </el-tag>
                      <span class="timeline-title">{{ item.title }}</span>
                    </div>
                    <p class="timeline-meta">操作人：{{ item.actor || '—' }}</p>
                  </div>
                </el-timeline-item>
              </el-timeline>

              <EmptyState
                v-else-if="!timelineLoading"
                title="暂无事件"
                description="时间轴只包含报价与订单事件；联系人维护等操作不会进入时间轴。"
              />
            </div>

            <div class="timeline-foot">
              <el-button
                v-if="timelineHasMore"
                size="small"
                :loading="timelineLoading"
                @click="loadMoreTimeline"
              >
                加载更多
              </el-button>
              <span v-else-if="timelineItems.length" class="subtle">
                已加载全部事件
              </span>
            </div>
          </section>
        </div>

        <aside class="detail-column">
          <section class="card card-pad">
            <div class="section-title">
              <h3>联系人</h3>
              <el-button
                v-if="canWrite"
                size="small"
                :icon="Plus"
                @click="contactVisible = true"
              >
                新增
              </el-button>
            </div>

            <div v-if="detail.contacts.length" class="contacts">
              <div v-for="contact in detail.contacts" :key="contact.id" class="contact-row">
                <span class="contact-avatar">{{ initials(contact.name) }}</span>
                <div class="contact-main">
                  <p class="contact-name">
                    {{ contact.name }}
                    <el-tag
                      v-if="contact.isDecisionMaker"
                      size="small"
                      type="success"
                      effect="light"
                      round
                    >
                      决策人
                    </el-tag>
                  </p>
                  <p class="contact-meta">
                    {{ [contact.position, contact.language].filter(Boolean).join(' · ') || '—' }}
                  </p>
                  <p class="contact-links">
                    <a v-if="contact.email" :href="`mailto:${contact.email}`">
                      {{ contact.email }}
                    </a>
                    <a v-if="contact.phone" :href="`tel:${contact.phone}`">
                      {{ contact.phone }}
                    </a>
                    <span v-if="contact.imAccount" class="subtle">
                      {{ contact.imType || 'IM' }}: {{ contact.imAccount }}
                    </span>
                  </p>
                </div>
                <el-button
                  v-if="canWrite"
                  link
                  type="danger"
                  @click="removeContact(contact)"
                >
                  删除
                </el-button>
              </div>
            </div>

            <EmptyState
              v-else
              title="暂无联系人"
              description="补充采购、技术等关键联系人，便于后续报价跟进。"
            >
              <el-button
                v-if="canWrite"
                size="small"
                type="primary"
                @click="contactVisible = true"
              >
                新增联系人
              </el-button>
            </EmptyState>
          </section>

          <section class="card card-pad">
            <div class="section-title">
              <h3>价格摘要</h3>
            </div>
            <div class="mini-stats">
              <div class="mini-stat">
                <span class="mini-value mono">{{ detail.priceSummary.activeItems }}</span>
                <span class="mini-label">生效中价格</span>
              </div>
              <div class="mini-stat">
                <span class="mini-value mono">{{ detail.priceSummary.expiringSoon }}</span>
                <span class="mini-label">即将到期</span>
              </div>
            </div>
            <p class="card-note">数值来自客户详情聚合；价格表明细接口尚未实现。</p>
          </section>

          <section class="card card-pad">
            <div class="section-title">
              <h3>订单</h3>
            </div>
            <div class="mini-stats">
              <div class="mini-stat">
                <span class="mini-value mono">{{ detail.orderCount }}</span>
                <span class="mini-label">累计订单</span>
              </div>
              <div class="mini-stat">
                <span class="mini-value mono small">{{ formatDate(detail.lastOrderAt) }}</span>
                <span class="mini-label">最近下单</span>
              </div>
            </div>
            <p class="card-note">订单明细接口尚未实现，此处只展示聚合字段。</p>
          </section>
        </aside>
      </div>

      <CustomerFormDialog
        v-model:visible="editVisible"
        mode="edit"
        :initial="detail"
        @saved="handleEdited"
      />
      <ContactFormDialog
        v-model:visible="contactVisible"
        :customer-id="detail.id"
        @saved="handleContactSaved"
      />
    </template>
  </div>
</template>

<style scoped>
.hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
  background:
    linear-gradient(120deg, #ffffff 0%, #ffffff 55%, #f4f8ff 100%);
}

.hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.hero-avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 56px;
  height: 56px;
  border-radius: 16px;
  background: var(--primary-soft);
  color: var(--primary);
  font-size: 17px;
  font-weight: 600;
}

.hero-text {
  min-width: 0;
}

.hero-name {
  font-size: 22px;
  font-weight: 600;
  line-height: 1.3;
  letter-spacing: 0.01em;
}

.hero-en {
  color: var(--text-2);
  font-size: 12.5px;
}

.hero-chips {
  margin-top: 10px;
}

.hero-metrics {
  display: flex;
  align-items: center;
  gap: 26px;
  padding-left: 8px;
}

.hero-metric {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.hero-metric-value {
  color: var(--text);
  font-size: 16px;
  font-weight: 600;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hero-metric-label {
  color: var(--text-3);
  font-size: 11.5px;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) minmax(0, 0.85fr);
  gap: 16px;
  align-items: start;
}

.detail-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.desc {
  --el-descriptions-item-bordered-label-background: var(--surface-2);
}

.desc :deep(.el-descriptions__label) {
  color: var(--text-3);
  font-weight: 500;
  font-size: 12px;
}

.desc :deep(.el-descriptions__content) {
  color: var(--text);
  font-size: 13px;
}

.card-note {
  margin-top: 12px;
  color: var(--text-3);
  font-size: 11.5px;
  line-height: 1.7;
}

.contacts {
  display: flex;
  flex-direction: column;
}

.contact-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 0;
  border-bottom: 1px dashed var(--border);
}

.contact-row:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

.contact-avatar {
  display: grid;
  place-items: center;
  flex: none;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--surface-2);
  color: var(--text-2);
  font-size: 11.5px;
  font-weight: 600;
}

.contact-main {
  flex: 1;
  min-width: 0;
}

.contact-name {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
}

.contact-meta {
  color: var(--text-3);
  font-size: 11.5px;
  margin-top: 1px;
}

.contact-links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
  margin-top: 4px;
  font-size: 11.5px;
}

.contact-links a {
  color: var(--text-2);
  overflow: hidden;
  text-overflow: ellipsis;
}

.contact-links a:hover {
  color: var(--primary);
}

.mini-stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.mini-stat {
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: 12px 14px;
  border-radius: 10px;
  background: var(--surface-2);
}

.mini-value {
  color: var(--text);
  font-size: 20px;
  font-weight: 600;
  line-height: 1.3;
}

.mini-value.small {
  font-size: 13.5px;
}

.mini-label {
  color: var(--text-3);
  font-size: 11.5px;
}

.timeline-body {
  min-height: 60px;
}

.timeline-card {
  padding: 10px 14px;
  border-radius: 10px;
  background: var(--surface-2);
}

.timeline-head {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.timeline-title {
  font-size: 13px;
  font-weight: 500;
}

.timeline-meta {
  margin-top: 4px;
  color: var(--text-3);
  font-size: 11.5px;
}

.timeline-alert {
  margin-bottom: 12px;
}

.timeline-foot {
  display: flex;
  justify-content: center;
  padding-top: 8px;
}

@media (max-width: 1040px) {
  .detail-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .hero {
    align-items: flex-start;
  }

  .hero-metrics {
    width: 100%;
    justify-content: space-between;
    padding-left: 0;
  }
}
</style>