<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { banPassenger, listPassengers } from '@/api/admin'
import type { PassengerSummary } from '@/types/api'

const loading = ref(false)
const rows = ref<PassengerSummary[]>([])
const total = ref(0)

const query = reactive({
  page: 1,
  size: 10,
})

async function load() {
  loading.value = true
  try {
    const res = await listPassengers({ page: query.page, size: query.size })
    rows.value = res.data.data.records
    total.value = res.data.data.total
  } finally {
    loading.value = false
  }
}

async function onBan(row: PassengerSummary, banned: boolean) {
  const action = banned ? '封禁' : '解封'
  await ElMessageBox.confirm(`确认${action}乘客 ${row.username}？`, '操作确认')
  await banPassenger(row.id, banned)
  ElMessage.success(`${action}成功`)
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
    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="creditScore" label="信用分" width="100" />
      <el-table-column prop="balance" label="余额(元)" width="120" />
      <el-table-column prop="createdAt" label="注册时间" min-width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link @click="onBan(row, true)">封禁</el-button>
          <el-button type="primary" link @click="onBan(row, false)">解封</el-button>
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
