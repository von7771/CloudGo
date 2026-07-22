<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getTripReceiptUrl, listTrips } from '@/api/admin'
import request from '@/utils/request'
import type { TripSummary } from '@/types/api'

const loading = ref(false)
const rows = ref<TripSummary[]>([])
const total = ref(0)

const query = reactive({
  status: '',
  page: 1,
  size: 10,
})

const statusOptions = [
  { label: '全部', value: '' },
  { label: 'CREATED', value: 'CREATED' },
  { label: 'DISPATCHING', value: 'DISPATCHING' },
  { label: 'ACCEPTED', value: 'ACCEPTED' },
  { label: 'IN_PROGRESS', value: 'IN_PROGRESS' },
  { label: 'COMPLETED', value: 'COMPLETED' },
  { label: 'CANCELLED', value: 'CANCELLED' },
]

async function load() {
  loading.value = true
  try {
    const res = await listTrips({
      status: query.status || undefined,
      page: query.page,
      size: query.size,
    })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

function onPageChange(page: number) {
  query.page = page
  load()
}

async function openReceipt(row: TripSummary) {
  const res = await getTripReceiptUrl(row.id)
  const url = res.data.data.receiptUrl
  if (!url) {
    ElMessage.warning('暂无电子凭证（需完单后由 Kafka 消费者生成）')
    return
  }
  const blobRes = await request.get(url, { responseType: 'blob' })
  const blobUrl = URL.createObjectURL(blobRes.data)
  window.open(blobUrl, '_blank')
}

onMounted(load)
</script>

<template>
  <el-card shadow="never">
    <el-form :inline="true" @submit.prevent="load">
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 160px">
          <el-option v-for="o in statusOptions" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="passengerId" label="乘客" width="80" />
      <el-table-column prop="driverId" label="司机" width="80" />
      <el-table-column prop="startPoint" label="起点" min-width="140" show-overflow-tooltip />
      <el-table-column prop="endPoint" label="终点" min-width="140" show-overflow-tooltip />
      <el-table-column prop="status" label="状态" width="120" />
      <el-table-column prop="estimatedAmount" label="预估(元)" width="100" />
      <el-table-column prop="finalAmount" label="实付(元)" width="100" />
      <el-table-column prop="createdAt" label="创建时间" min-width="170" />
      <el-table-column label="凭证" width="100" fixed="right">
        <template #default="{ row }">
          <el-button
            v-if="row.status === 'COMPLETED'"
            link
            type="primary"
            @click="openReceipt(row)"
          >
            电子凭证
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
  </el-card>
</template>

<style scoped>
.pager {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
