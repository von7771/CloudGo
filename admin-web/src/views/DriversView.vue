<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditDriver, listDrivers } from '@/api/admin'
import request from '@/utils/request'
import type { DriverSummary } from '@/types/api'

const loading = ref(false)
const rows = ref<DriverSummary[]>([])
const total = ref(0)
const previewVisible = ref(false)
const previewUrl = ref('')
const previewTitle = ref('证件预览')

const query = reactive({
  auditStatus: '',
  page: 1,
  size: 10,
})

async function load() {
  loading.value = true
  try {
    const res = await listDrivers({
      auditStatus: query.auditStatus || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

async function openPreview(url: string | null | undefined, title: string) {
  if (!url) {
    ElMessage.warning('暂无证件图片')
    return
  }
  try {
    const res = await request.get(url, { responseType: 'blob' })
    previewUrl.value = URL.createObjectURL(res.data)
    previewTitle.value = title
    previewVisible.value = true
  } catch {
    ElMessage.error('加载证件图片失败')
  }
}

async function onAudit(row: DriverSummary, auditStatus: string) {
  const label = auditStatus === 'APPROVED' ? '通过' : '拒绝'
  await ElMessageBox.confirm(`确认${label}司机 ${row.realName}？`, '审核')
  await auditDriver(row.id, auditStatus)
  ElMessage.success('审核完成')
  load()
}

function onPageChange(page: number) {
  query.page = page
  load()
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <el-form :inline="true">
      <el-form-item label="审核状态">
        <el-select v-model="query.auditStatus" clearable placeholder="全部" style="width: 140px">
          <el-option label="PENDING" value="PENDING" />
          <el-option label="APPROVED" value="APPROVED" />
          <el-option label="REJECTED" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column prop="realName" label="姓名" width="120" />
      <el-table-column prop="auditStatus" label="审核状态" width="120" />
      <el-table-column label="驾驶证" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row.licenseImageUrl, '驾驶证')">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column label="身份证" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openPreview(row.idCardImageUrl, '身份证')">查看</el-button>
        </template>
      </el-table-column>
      <el-table-column prop="balance" label="余额" width="100" />
      <el-table-column prop="createdAt" label="注册时间" min-width="170" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.auditStatus === 'PENDING'"
            type="success"
            link
            @click="onAudit(row, 'APPROVED')"
          >
            通过
          </el-button>
          <el-button
            v-if="row.auditStatus === 'PENDING'"
            type="danger"
            link
            @click="onAudit(row, 'REJECTED')"
          >
            拒绝
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pager">
      <el-pagination
        background
        layout="total, prev, pager, next"
        :total="total"
        :page-size="query.size"
        :current-page="query.page"
        @current-change="onPageChange"
      />
    </div>

    <el-dialog v-model="previewVisible" :title="previewTitle" width="520px">
      <img v-if="previewUrl" :src="previewUrl" alt="preview" class="preview-img" />
    </el-dialog>
  </el-card>
</template>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
.preview-img {
  width: 100%;
  max-height: 420px;
  object-fit: contain;
}
</style>
