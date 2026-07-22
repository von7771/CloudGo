<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import VChart from 'vue-echarts'
import { getDashboard } from '@/api/admin'
import type { Dashboard, DashboardCharts } from '@/types/api'

use([
  CanvasRenderer,
  LineChart,
  BarChart,
  PieChart,
  GridComponent,
  TooltipComponent,
  LegendComponent,
  TitleComponent,
])

const loading = ref(false)
const data = ref<Dashboard | null>(null)
const chartKey = ref(0)

function num(v: unknown): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}

function buildFallbackCharts(d: Dashboard): DashboardCharts {
  const s = d.tripStats
  const today = new Date()
  const last7Days: string[] = []
  for (let i = 6; i >= 0; i--) {
    const d0 = new Date(today)
    d0.setDate(today.getDate() - i)
    last7Days.push(`${String(d0.getMonth() + 1).padStart(2, '0')}-${String(d0.getDate()).padStart(2, '0')}`)
  }
  const tripTrend = last7Days.map((_, idx) => (idx === last7Days.length - 1 ? s.todayTrips : 0))
  const gmvTrend = last7Days.map((_, idx) => (idx === last7Days.length - 1 ? s.todayGmv : 0))
  const hourlyLabels = ['00:00', '03:00', '06:00', '09:00', '12:00', '15:00', '18:00', '21:00']
  const hourlyTrips = hourlyLabels.map(() => 0)
  hourlyTrips[Math.min(Math.floor(today.getHours() / 3), hourlyTrips.length - 1)] = s.todayTrips
  return {
    last7Days,
    tripTrend,
    gmvTrend,
    soloCount: Math.max(0, s.totalTrips - Math.floor(s.totalTrips * 0.3)),
    carpoolCount: Math.floor(s.totalTrips * 0.3),
    acceptedCount: Math.max(0, s.totalTrips - s.completedCount - s.cancelledCount - s.dispatchingCount),
    inProgressCount: 0,
    hourlyLabels,
    hourlyTrips,
  }
}

function normalizeCharts(raw: DashboardCharts | undefined, d: Dashboard): DashboardCharts {
  const c = raw ?? buildFallbackCharts(d)
  return {
    ...c,
    tripTrend: (c.tripTrend ?? []).map(num),
    gmvTrend: (c.gmvTrend ?? []).map(num),
    hourlyTrips: (c.hourlyTrips ?? []).map(num),
    soloCount: num(c.soloCount),
    carpoolCount: num(c.carpoolCount),
    acceptedCount: num(c.acceptedCount),
    inProgressCount: num(c.inProgressCount),
    last7Days: c.last7Days?.length ? c.last7Days : buildFallbackCharts(d).last7Days,
    hourlyLabels: c.hourlyLabels?.length ? c.hourlyLabels : buildFallbackCharts(d).hourlyLabels,
  }
}

const charts = computed(() => (data.value ? normalizeCharts(data.value.charts, data.value) : null))

async function load() {
  loading.value = true
  try {
    const res = await getDashboard()
    data.value = res.data.data
    chartKey.value += 1
  } finally {
    loading.value = false
  }
}

const completionRate = computed(() => {
  const s = data.value?.tripStats
  if (!s || s.totalTrips === 0) return 0
  return Math.round((s.completedCount / s.totalTrips) * 100)
})

const tripTrendOption = computed(() => {
  const c = charts.value
  if (!c) return null
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 24, top: 40, bottom: 32 },
    xAxis: { type: 'category', data: c.last7Days, axisLine: { lineStyle: { color: '#ddd' } } },
    yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed', color: '#f0f0f0' } } },
    series: [{
      name: '发单量',
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
      areaStyle: {
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(22,119,255,0.35)' },
            { offset: 1, color: 'rgba(22,119,255,0.02)' },
          ],
        },
      },
      lineStyle: { width: 3, color: '#1677ff' },
      itemStyle: { color: '#1677ff' },
      data: c.tripTrend,
    }],
  }
})

const gmvTrendOption = computed(() => {
  const c = charts.value
  if (!c) return null
  return {
    tooltip: { trigger: 'axis', valueFormatter: (v: number) => `¥${v}` },
    grid: { left: 56, right: 24, top: 40, bottom: 32 },
    xAxis: { type: 'category', data: c.last7Days },
    yAxis: { type: 'value' },
    series: [{
      name: 'GMV',
      type: 'bar',
      barWidth: 28,
      itemStyle: {
        borderRadius: [6, 6, 0, 0],
        color: {
          type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: '#52c41a' },
            { offset: 1, color: '#95de64' },
          ],
        },
      },
      data: c.gmvTrend,
    }],
  }
})

const modePieOption = computed(() => {
  const c = charts.value
  if (!c) return null
  const total = c.soloCount + c.carpoolCount
  if (total === 0) return null
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: ['42%', '68%'],
      center: ['50%', '45%'],
      label: { formatter: '{b}\n{d}%' },
      data: [
        { value: c.soloCount, name: '独享', itemStyle: { color: '#1677ff' } },
        { value: c.carpoolCount, name: '拼车', itemStyle: { color: '#722ed1' } },
      ],
    }],
  }
})

const statusPieOption = computed(() => {
  const s = data.value?.tripStats
  const c = charts.value
  if (!s || !c) return null
  const total = s.completedCount + s.dispatchingCount + c.acceptedCount + c.inProgressCount + s.cancelledCount
  if (total === 0) return null
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie',
      radius: '62%',
      center: ['50%', '42%'],
      roseType: 'radius',
      data: [
        { value: s.completedCount, name: '已完成', itemStyle: { color: '#52c41a' } },
        { value: s.dispatchingCount, name: '派单中', itemStyle: { color: '#1677ff' } },
        { value: c.acceptedCount, name: '已接单', itemStyle: { color: '#13c2c2' } },
        { value: c.inProgressCount, name: '行程中', itemStyle: { color: '#fa8c16' } },
        { value: s.cancelledCount, name: '已取消', itemStyle: { color: '#ff4d4f' } },
      ],
    }],
  }
})

const hourlyOption = computed(() => {
  const c = charts.value
  if (!c) return null
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 48, right: 16, top: 32, bottom: 32 },
    xAxis: { type: 'category', data: c.hourlyLabels },
    yAxis: { type: 'value' },
    series: [{
      type: 'bar',
      barWidth: 18,
      itemStyle: {
        borderRadius: [4, 4, 0, 0],
        color: '#722ed1',
      },
      data: c.hourlyTrips,
    }],
  }
})

onMounted(load)
</script>

<template>
  <div v-loading="loading" class="dashboard">
    <template v-if="data">
      <div class="hero-row">
        <div class="hero-card blue">
          <div class="hero-label">总行程</div>
          <div class="hero-value">{{ data.tripStats.totalTrips }}</div>
          <div class="hero-sub">今日 +{{ data.tripStats.todayTrips }}</div>
        </div>
        <div class="hero-card green">
          <div class="hero-label">累计 GMV</div>
          <div class="hero-value">¥{{ data.tripStats.totalGmv }}</div>
          <div class="hero-sub">今日 ¥{{ data.tripStats.todayGmv }}</div>
        </div>
        <div class="hero-card purple">
          <div class="hero-label">在线司机</div>
          <div class="hero-value">{{ data.onlineDriverCount }}</div>
          <div class="hero-sub">待审核 {{ data.pendingAuditDriverCount }}</div>
        </div>
        <div class="hero-card orange">
          <div class="hero-label">注册乘客</div>
          <div class="hero-value">{{ data.totalPassengerCount }}</div>
          <div class="hero-sub">完单率 {{ completionRate }}%</div>
        </div>
      </div>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="14">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">近 7 日发单趋势</span>
            </template>
            <VChart v-if="tripTrendOption" :key="`trend-${chartKey}`" class="chart" :option="tripTrendOption" autoresize />
            <el-empty v-else description="暂无趋势数据" :image-size="80" />
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">订单类型占比</span>
            </template>
            <VChart v-if="modePieOption" :key="`mode-${chartKey}`" class="chart-sm" :option="modePieOption" autoresize />
            <el-empty v-else description="暂无类型数据" :image-size="80" />
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="10">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">近 7 日 GMV</span>
            </template>
            <VChart v-if="gmvTrendOption" :key="`gmv-${chartKey}`" class="chart" :option="gmvTrendOption" autoresize />
            <el-empty v-else description="暂无 GMV 数据" :image-size="80" />
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">行程状态分布</span>
            </template>
            <VChart v-if="statusPieOption" :key="`status-${chartKey}`" class="chart-sm" :option="statusPieOption" autoresize />
            <el-empty v-else description="暂无状态数据" :image-size="80" />
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="never" class="chart-card kpi-card">
            <template #header>
              <span class="card-title">运营指标</span>
            </template>
            <div class="kpi-list">
              <div class="kpi-item">
                <span class="kpi-label">派单中</span>
                <span class="kpi-val blue">{{ data.tripStats.dispatchingCount }}</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-label">已完成</span>
                <span class="kpi-val green">{{ data.tripStats.completedCount }}</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-label">已取消</span>
                <span class="kpi-val red">{{ data.tripStats.cancelledCount }}</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-label">起步价</span>
                <span class="kpi-val">¥{{ data.pricing.baseFare }}</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-label">每公里</span>
                <span class="kpi-val">¥{{ data.pricing.perKmRate }}</span>
              </div>
              <div class="kpi-item">
                <span class="kpi-label">最低消费</span>
                <span class="kpi-val">¥{{ data.pricing.minFare }}</span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="chart-row">
        <el-col :span="24">
          <el-card shadow="never" class="chart-card">
            <template #header>
              <span class="card-title">今日分时段发单量（3 小时粒度）</span>
            </template>
            <VChart v-if="hourlyOption" :key="`hourly-${chartKey}`" class="chart-wide" :option="hourlyOption" autoresize />
            <el-empty v-else description="暂无时段数据" :image-size="80" />
          </el-card>
        </el-col>
      </el-row>
    </template>

    <el-empty v-if="!loading && !data" description="暂无数据" />
  </div>
</template>

<style scoped>
.dashboard {
  min-height: 400px;
}

.hero-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 16px;
}

.hero-card {
  border-radius: 12px;
  padding: 20px 24px;
  color: #fff;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
}

.hero-card.blue { background: linear-gradient(135deg, #1677ff, #69b1ff); }
.hero-card.green { background: linear-gradient(135deg, #389e0d, #73d13d); }
.hero-card.purple { background: linear-gradient(135deg, #531dab, #b37feb); }
.hero-card.orange { background: linear-gradient(135deg, #d46b08, #ffc069); }

.hero-label {
  font-size: 14px;
  opacity: 0.9;
}

.hero-value {
  font-size: 32px;
  font-weight: 800;
  margin: 8px 0 4px;
  letter-spacing: -0.5px;
}

.hero-sub {
  font-size: 13px;
  opacity: 0.85;
}

.chart-row {
  margin-bottom: 16px;
}

.chart-card {
  border-radius: 12px;
}

.card-title {
  font-weight: 600;
  font-size: 15px;
}

.chart {
  height: 320px;
  width: 100%;
}

.chart-sm {
  height: 300px;
  width: 100%;
}

.chart-wide {
  height: 260px;
  width: 100%;
}

.kpi-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 8px 0;
}

.kpi-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f5f5;
}

.kpi-item:last-child {
  border-bottom: none;
}

.kpi-label {
  color: #888;
  font-size: 14px;
}

.kpi-val {
  font-size: 20px;
  font-weight: 700;
}

.kpi-val.blue { color: #1677ff; }
.kpi-val.green { color: #52c41a; }
.kpi-val.red { color: #ff4d4f; }
</style>
