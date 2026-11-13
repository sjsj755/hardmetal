<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ApiError, errorMessage } from '../api/http'
import { createCustomer, updateCustomer } from '../api/customer'
import { fetchUserOptions } from '../api/user'
import type {
  CreatedCustomer,
  CustomerDetailView,
  CustomerUpsertPayload,
  UserOption
} from '../api/types'
import { ROLE_ADMIN, ROLE_SALES } from '../domain/roles'
import { useAuthStore } from '../stores/auth'
import { useDictStore } from '../stores/dict'
import { MACHINING_MATERIALS, MACHINING_OPS } from '../domain/dicts'

interface FormModel {
  code: string
  nameCn: string
  nameEn: string
  customerType: string
  level: string
  countryCode: string
  currency: string
  machiningMaterials: string[]
  machiningOps: string[]
  machineBrands: string[]
  benchmarkBrand: string
  paymentTerms: string
  creditDays: number | undefined
  creditLimit: string
  ownerId: number | null
  remark: string
}

const props = withDefaults(
  defineProps<{
    visible: boolean
    mode?: 'create' | 'edit'
    initial?: CustomerDetailView | null
  }>(),
  { mode: 'create', initial: null }
)

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', payload?: CreatedCustomer): void
}>()

const dict = useDictStore()
const auth = useAuthStore()
const formRef = ref<FormInstance>()
const form = reactive<FormModel>(emptyForm())
const submitting = ref(false)
const errorText = ref('')

const ownerOptions = ref<UserOption[]>([])
const ownerOptionsLoading = ref(false)
const ownerOptionsError = ref('')

const isAdmin = computed(() => auth.hasRole(ROLE_ADMIN))
const isSales = computed(() => auth.hasRole(ROLE_SALES))
const currentUserId = computed(() => auth.user?.id ?? null)

const customerTypes = computed(() => dict.items('customer_type'))
const grades = computed(() => dict.items('customer_grade'))
const currencies = computed(() => dict.items('currency'))

/** 编辑时若原归属不在 SALES 下拉里（例如早先默认归 admin），保留一个当前归属选项。 */
const ownerChoices = computed<UserOption[]>(() => {
  const list = [...ownerOptions.value]
  const initial = props.initial
  const ownerId = initial?.ownerId
  if (props.mode === 'edit' && ownerId != null && !list.some((item) => item.id === ownerId)) {
    list.unshift({
      id: ownerId,
      username: '',
      realName: initial?.ownerName ?? '当前归属',
      roleCode: ''
    })
  }
  // SALES 不下拉用户列表，这里补一条“自己”，让禁用输入框能显示姓名而不是占位符。
  if (isSales.value) {
    const userId = currentUserId.value
    if (userId != null && !list.some((item) => item.id === userId)) {
      list.unshift({
        id: userId,
        username: auth.user?.username ?? '',
        realName: auth.user?.realName ?? null,
        roleCode: ROLE_SALES
      })
    }
  }
  return list
})

function ownerOptionLabel(option: UserOption): string {
  return option.realName || option.username || `#${option.id}`
}

async function loadOwnerOptions(): Promise<void> {
  if (!isAdmin.value) {
    return
  }
  ownerOptionsLoading.value = true
  ownerOptionsError.value = ''
  try {
    const result = await fetchUserOptions('SALES')
    ownerOptions.value = result.items
  } catch (error) {
    ownerOptions.value = []
    ownerOptionsError.value = errorMessage(error)
  } finally {
    ownerOptionsLoading.value = false
  }
}

const rules: FormRules = {
  code: [
    { required: true, message: '请输入客户编码', trigger: 'blur' },
    { max: 32, message: '最长 32 个字符', trigger: 'blur' }
  ],
  nameCn: [
    { required: true, message: '请输入客户中文名', trigger: 'blur' },
    { max: 128, message: '最长 128 个字符', trigger: 'blur' }
  ],
  customerType: [{ required: true, message: '请选择客户类型', trigger: 'change' }],
  countryCode: [
    { required: true, message: '请输入国家代码', trigger: 'blur' },
    { pattern: /^[A-Za-z]{2}$/, message: '两位字母，如 DE', trigger: 'blur' }
  ],
  currency: [{ pattern: /^[A-Za-z]{3}$/, message: '三位字母，如 EUR', trigger: 'change' }]
}

function emptyForm(): FormModel {
  return {
    code: '',
    nameCn: '',
    nameEn: '',
    customerType: '',
    level: '',
    countryCode: '',
    currency: 'USD',
    machiningMaterials: [],
    machiningOps: [],
    machineBrands: [],
    benchmarkBrand: '',
    paymentTerms: '',
    creditDays: undefined,
    creditLimit: '',
    ownerId: null,
    remark: ''
  }
}

function fillFrom(detail: CustomerDetailView): void {
  Object.assign(form, {
    code: detail.code,
    nameCn: detail.nameCn ?? '',
    nameEn: detail.nameEn ?? '',
    customerType: detail.customerType,
    level: detail.level ?? '',
    countryCode: detail.countryCode,
    currency: detail.currency ?? 'USD',
    machiningMaterials: [...detail.machiningMaterials],
    machiningOps: [...detail.machiningOps],
    machineBrands: [...detail.machineBrands],
    benchmarkBrand: detail.benchmarkBrand ?? '',
    paymentTerms: detail.paymentTerms ?? '',
    creditDays: detail.creditDays ?? undefined,
    creditLimit: detail.creditLimit ?? '',
    ownerId: detail.ownerId,
    remark: detail.remark ?? ''
  })
}

watch(
  () => props.visible,
  async (open) => {
    if (!open) {
      return
    }
    errorText.value = ''
    ownerOptions.value = []
    ownerOptionsError.value = ''
    Object.assign(form, emptyForm())
    if (props.mode === 'edit' && props.initial) {
      fillFrom(props.initial)
    }
    // SALES 只能归到自己名下：固定当前登录员工，禁止改派。
    if (isSales.value) {
      form.ownerId = currentUserId.value
    }
    const tasks: Array<Promise<unknown>> = ['customer_type', 'customer_grade', 'currency'].map(
      (type) => dict.load(type).catch(() => null)
    )
    if (isAdmin.value) {
      tasks.push(loadOwnerOptions())
    }
    await Promise.all(tasks)
    await nextTick()
    formRef.value?.clearValidate()
  }
)

/** 只发送要改的字段；ownerId 为空表示保留原归属，remark 始终发送以支持清空。 */
function buildPayload(): CustomerUpsertPayload {
  const payload: CustomerUpsertPayload = {
    code: form.code.trim(),
    nameCn: form.nameCn.trim(),
    nameEn: form.nameEn.trim(),
    customerType: form.customerType,
    countryCode: form.countryCode.trim().toUpperCase(),
    machiningMaterials: [...form.machiningMaterials],
    machiningOps: [...form.machiningOps],
    machineBrands: [...form.machineBrands]
  }
  if (form.level) {
    payload.level = form.level
  }
  if (form.currency) {
    payload.currency = form.currency
  }
  if (form.benchmarkBrand.trim()) {
    payload.benchmarkBrand = form.benchmarkBrand.trim()
  }
  if (form.paymentTerms.trim()) {
    payload.paymentTerms = form.paymentTerms.trim()
  }
  if (form.creditDays !== undefined) {
    payload.creditDays = form.creditDays
  }
  if (form.creditLimit.trim()) {
    payload.creditLimit = form.creditLimit.trim()
  }
  if (form.ownerId != null) {
    payload.ownerId = form.ownerId
  }
  // 备注始终发送：空字符串用于清空；上限 2000 由输入框与后端共同约束。
  payload.remark = form.remark
  return payload
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  const creditLimit = form.creditLimit.trim()
  if (creditLimit && !/^\d+(\.\d{1,4})?$/.test(creditLimit)) {
    errorText.value = '授信额度格式不正确：最多四位小数（对应 numeric(18,4)）'
    return
  }
  if (isAdmin.value && props.mode === 'create' && form.ownerId == null) {
    errorText.value = '请选择归属员工'
    return
  }
  if (isSales.value && form.ownerId == null) {
    errorText.value = '无法获取当前登录员工，请重新登录后再试'
    return
  }
  submitting.value = true
  errorText.value = ''
  try {
    if (props.mode === 'edit') {
      if (!props.initial) {
        return
      }
      await updateCustomer(props.initial.id, buildPayload())
      emit('saved')
    } else {
      const created = await createCustomer(buildPayload())
      emit('saved', created)
    }
    emit('update:visible', false)
  } catch (error) {
    errorText.value =
      error instanceof ApiError
        ? error.detail?.reason || error.message
        : '保存失败，请稍后重试'
  } finally {
    submitting.value = false
  }
}

function onVisibleChange(value: boolean): void {
  emit('update:visible', value)
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="mode === 'create' ? '新建客户' : '编辑客户'"
    width="720px"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <el-alert
        v-if="isAdmin && ownerOptionsError"
        :title="`归属员工加载失败：${ownerOptionsError}`"
        type="error"
        :closable="false"
        show-icon
        class="form-error"
      >
        <template #default>
          <el-button link type="primary" @click="loadOwnerOptions">重新加载</el-button>
        </template>
      </el-alert>

      <el-alert
        v-else-if="isAdmin && !ownerOptionsLoading && mode === 'create' && ownerChoices.length === 0"
        title="暂无启用的员工账号，无法指定归属"
        type="warning"
        :closable="false"
        show-icon
        class="form-error"
      />

      <div class="form-grid">
        <el-form-item label="客户编码" prop="code">
          <el-input v-model="form.code" maxlength="32" placeholder="如 DE-0007" />
        </el-form-item>
        <el-form-item label="客户类型" prop="customerType">
          <el-select v-model="form.customerType" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="item in customerTypes"
              :key="item.value"
              :label="item.labelZh || item.value"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item
          label="归属员工"
          class="span-2"
          :required="isAdmin && mode === 'create'"
        >
          <el-select
            v-model="form.ownerId"
            :disabled="!isAdmin"
            :loading="ownerOptionsLoading"
            :placeholder="isAdmin ? '请选择归属员工' : '当前登录员工'"
            style="width: 100%"
          >
            <el-option
              v-for="item in ownerChoices"
              :key="item.id"
              :label="ownerOptionLabel(item)"
              :value="item.id"
            />
          </el-select>
          <p v-if="!isAdmin" class="field-hint">归属固定为当前登录员工，不可修改。</p>
          <p v-else-if="mode === 'edit'" class="field-hint">
            可改派给其他员工；不选择则保留原归属。
          </p>
          <p v-else class="field-hint">新建客户必须选择归属员工，创建后可在列表中按归属筛选。</p>
        </el-form-item>

        <el-form-item label="中文名称" prop="nameCn">
          <el-input v-model="form.nameCn" maxlength="128" placeholder="如 德国精密刀具贸易公司" />
        </el-form-item>
        <el-form-item label="英文名称">
          <el-input v-model="form.nameEn" maxlength="128" placeholder="如 Mueller Werkzeuge GmbH" />
        </el-form-item>

        <el-form-item label="国家代码" prop="countryCode">
          <el-input
            v-model="form.countryCode"
            maxlength="2"
            class="mono"
            placeholder="两位字母，如 DE"
          />
        </el-form-item>
        <el-form-item label="客户等级">
          <el-select v-model="form.level" clearable placeholder="未评级" style="width: 100%">
            <el-option
              v-for="item in grades"
              :key="item.value"
              :label="item.labelZh || item.value"
              :value="item.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="结算币种" prop="currency">
          <el-select v-model="form.currency" placeholder="请选择" style="width: 100%">
            <el-option
              v-for="item in currencies"
              :key="item.value"
              :label="item.labelZh || item.value"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="账期（天）">
          <el-input-number
            v-model="form.creditDays"
            :min="0"
            :max="365"
            :step="1"
            controls-position="right"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="授信额度">
          <el-input
            v-model="form.creditLimit"
            class="mono"
            placeholder="数字，如 50000.00（最多四位小数）"
          />
        </el-form-item>
        <el-form-item label="对标品牌">
          <el-input v-model="form.benchmarkBrand" maxlength="64" placeholder="如 Sandvik" />
        </el-form-item>

        <el-form-item label="付款条款" class="span-2">
          <el-input
            v-model="form.paymentTerms"
            maxlength="32"
            placeholder="自由文本，如 30% T/T in advance"
          />
        </el-form-item>

        <el-form-item label="加工材料" class="span-2">
          <el-select
            v-model="form.machiningMaterials"
            multiple
            filterable
            allow-create
            default-first-option
            :multiple-limit="16"
            placeholder="选择或输入材料代码，如 stainless"
            style="width: 100%"
          >
            <el-option v-for="item in MACHINING_MATERIALS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="加工工序" class="span-2">
          <el-select
            v-model="form.machiningOps"
            multiple
            filterable
            allow-create
            default-first-option
            :multiple-limit="16"
            placeholder="选择或输入工序代码，如 milling"
            style="width: 100%"
          >
            <el-option v-for="item in MACHINING_OPS" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>

        <el-form-item label="机床品牌" class="span-2">
          <el-select
            v-model="form.machineBrands"
            multiple
            filterable
            allow-create
            default-first-option
            :reserve-keyword="false"
            placeholder="输入品牌后回车，如 DMG Mori"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注" class="span-2">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="内部备注，最多 2000 字；可清空"
          />
        </el-form-item>
      </div>

      <el-alert
        v-if="errorText"
        :title="errorText"
        type="error"
        :closable="false"
        show-icon
        class="form-error"
      />
    </el-form>

    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.span-2 {
  grid-column: span 2;
}

.form-error {
  margin-top: 4px;
}

.field-hint {
  margin: 4px 0 0;
  color: var(--text-3);
  font-size: 11.5px;
  line-height: 1.6;
}

@media (max-width: 640px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .span-2 {
    grid-column: span 1;
  }
}
</style>