<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ApiError } from '../api/http'
import { addContact } from '../api/customer'
import type { ContactPayload, ContactView } from '../api/types'

const props = defineProps<{
  visible: boolean
  customerId: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'saved', contact: ContactView): void
}>()

interface ContactFormModel {
  name: string
  position: string
  email: string
  phone: string
  imType: string
  imAccount: string
  language: string
  isDecisionMaker: boolean
}

const formRef = ref<FormInstance>()
const form = reactive<ContactFormModel>(emptyForm())
const submitting = ref(false)
const errorText = ref('')

const rules: FormRules = {
  name: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { max: 64, message: '最长 64 个字符', trigger: 'blur' }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

function emptyForm(): ContactFormModel {
  return {
    name: '',
    position: '',
    email: '',
    phone: '',
    imType: '',
    imAccount: '',
    language: '',
    isDecisionMaker: false
  }
}

watch(
  () => props.visible,
  async (open) => {
    if (!open) {
      return
    }
    Object.assign(form, emptyForm())
    errorText.value = ''
    await nextTick()
    formRef.value?.clearValidate()
  }
)

function buildPayload(): ContactPayload {
  const payload: ContactPayload = { name: form.name.trim() }
  if (form.position.trim()) {
    payload.position = form.position.trim()
  }
  if (form.email.trim()) {
    payload.email = form.email.trim()
  }
  if (form.phone.trim()) {
    payload.phone = form.phone.trim()
  }
  if (form.imType.trim()) {
    payload.imType = form.imType.trim()
  }
  if (form.imAccount.trim()) {
    payload.imAccount = form.imAccount.trim()
  }
  if (form.language.trim()) {
    payload.language = form.language.trim()
  }
  payload.isDecisionMaker = form.isDecisionMaker
  return payload
}

async function submit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    return
  }
  submitting.value = true
  errorText.value = ''
  try {
    const contact = await addContact(props.customerId, buildPayload())
    emit('saved', contact)
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
    title="新增联系人"
    width="560px"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
      <div class="form-grid">
        <el-form-item label="姓名" prop="name">
          <el-input v-model="form.name" maxlength="64" placeholder="如 Thomas Müller" />
        </el-form-item>
        <el-form-item label="职位">
          <el-input v-model="form.position" maxlength="32" placeholder="如 采购经理" />
        </el-form-item>

        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" maxlength="128" placeholder="name@example.com" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model="form.phone" maxlength="32" placeholder="+49 89 123456" />
        </el-form-item>

        <el-form-item label="即时通讯类型">
          <el-input v-model="form.imType" maxlength="16" placeholder="自由文本，如 whatsapp" />
        </el-form-item>
        <el-form-item label="即时通讯账号">
          <el-input v-model="form.imAccount" maxlength="64" placeholder="号码或用户名" />
        </el-form-item>

        <el-form-item label="沟通语言">
          <el-input v-model="form.language" maxlength="16" placeholder="如 en / de" />
        </el-form-item>
        <el-form-item label="是否决策人">
          <el-switch v-model="form.isDecisionMaker" />
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

.form-error {
  margin-top: 4px;
}

@media (max-width: 560px) {
  .form-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>