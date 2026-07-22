<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getPricing, updatePricing } from '@/api/admin'

const loading = ref(false)
const saving = ref(false)

const form = reactive({
  baseFare: 5,
  perKmRate: 1.5,
  minFare: 10,
})

async function load() {
  loading.value = true
  try {
    const res = await getPricing()
    Object.assign(form, res.data.data)
  } finally {
    loading.value = false
  }
}

async function onSave() {
  saving.value = true
  try {
    await updatePricing({ ...form })
    ElMessage.success('计价规则已更新')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <el-alert
    title="天气动态调价（起点天气，已自动计入乘客发单/路线规划）"
    type="info"
    :closable="false"
    show-icon
    style="margin-bottom: 16px; max-width: 720px"
  >
    <template #default>
      <p style="margin: 4px 0">晴/多云 1.0 · 小雨 1.10 · 雨 1.15 · 雷雨 1.25 · 雪 1.30 · 雾霾 1.10</p>
      <p style="margin: 4px 0">高温≥35℃ 再×1.05 · 低温≤0℃ 再×1.10 · 上限 1.50</p>
      <p style="margin: 4px 0">测试：GET /api/map/weather?lat=23.13&lon=113.27</p>
    </template>
  </el-alert>

  <el-card shadow="never" v-loading="loading" style="max-width: 480px">
    <el-form label-width="100px">
      <el-form-item label="起步价(元)">
        <el-input-number v-model="form.baseFare" :min="0" :step="1" :precision="2" />
      </el-form-item>
      <el-form-item label="每公里(元)">
        <el-input-number v-model="form.perKmRate" :min="0" :step="0.1" :precision="2" />
      </el-form-item>
      <el-form-item label="最低消费(元)">
        <el-input-number v-model="form.minFare" :min="0" :step="1" :precision="2" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="saving" @click="onSave">保存</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>
