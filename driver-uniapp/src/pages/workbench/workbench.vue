<template>
  <view class="page">
    <view class="header">
      <view class="driver-info">
        <view class="avatar">{{ avatarText }}</view>
        <view>
          <text class="name">{{ realName || '司机' }}</text>
          <text class="sub">ID #{{ driverId }}</text>
        </view>
      </view>
      <view class="online-switch">
        <button class="ai-btn" size="mini" @click="goAssistant">AI助手</button>
        <text :class="['switch-label', online ? 'on' : '']">{{ online ? '接单中' : '已休息' }}</text>
        <switch :checked="online" color="#52c41a" @change="onToggleOnline" />
      </view>
    </view>

    <view class="stats-row">
      <view class="stat-card">
        <text class="stat-val">{{ pending.length }}</text>
        <text class="stat-label">待接单</text>
      </view>
      <view class="stat-card">
        <text class="stat-val">{{ todayCompleted }}</text>
        <text class="stat-label">今日完单</text>
      </view>
      <view class="stat-card">
        <text class="stat-val">{{ locCountdown }}s</text>
        <text class="stat-label">下次上报</text>
      </view>
    </view>

    <view v-if="online" class="status-strip">
      <view class="strip-item">
        <text class="strip-dot" :class="{ on: wsConnected }" />
        <text>{{ wsConnected ? '新单实时推送' : '新单轮询刷新（10 秒）' }}</text>
      </view>
      <view class="strip-item">
        <text class="strip-icon">📍</text>
        <text>{{ lastLocation || '位置未上报' }}</text>
      </view>
    </view>

    <view v-if="activeTrips.length" class="section-title">
      <text>进行中行程（{{ activeTrips.length }}）</text>
    </view>

    <view v-for="trip in activeTrips" :key="trip.id" class="card active-trip">
      <view class="card-head">
        <text class="card-title">#{{ trip.id }} {{ trip.tripMode === 'CARPOOL' ? '拼车' : '独享' }}</text>
        <StatusTag :status="trip.status" />
      </view>
      <text class="route">🟢 {{ trip.startPoint }}</text>
      <text class="route-arrow">↓</text>
      <text class="route">🔴 {{ trip.endPoint }}</text>
      <text class="fare">预估 ¥{{ trip.estimatedAmount }} · {{ formatDistance(trip.distanceMeters) }}</text>

      <view class="step-bar">
        <view
          v-for="(step, i) in driverSteps"
          :key="step.key"
          :class="['step', stepClassFor(trip.status, step.key)]"
        >
          <text class="step-dot">{{ i + 1 }}</text>
          <text class="step-label">{{ step.label }}</text>
        </view>
      </view>

      <view class="action-row">
        <button v-if="trip.status === 'ACCEPTED'" class="act primary" @click="onArrive(trip.id)">到达上车点</button>
        <button v-if="trip.status === 'ARRIVED'" class="act primary" @click="onStart(trip.id)">乘客已上车</button>
        <button v-if="trip.status === 'IN_PROGRESS'" class="act complete" @click="onComplete(trip.id)">确认完单</button>
      </view>
    </view>

    <view class="card smart-card">
      <view class="card-head">
        <text class="card-title">🤖 智能拼车包</text>
        <button size="mini" @click="loadSmartBundles">刷新</button>
      </view>
      <text class="smart-hint">AI 路线匹配 · 最多 3 单同向 · 一键接单</text>
      <view v-if="!online" class="empty">上线后可见推荐</view>
      <view v-else-if="smartBundles.length === 0" class="empty">暂无同向打包推荐</view>
      <view v-for="bundle in smartBundles" :key="bundle.bundleId" class="bundle-card">
        <text class="bundle-summary">{{ bundle.summary }}</text>
        <text class="bundle-fare">合计 ¥{{ bundle.totalEstimatedFare }} · 相似度 {{ formatPercent(bundle.similarityScore) }}</text>
        <view v-for="t in bundle.trips" :key="t.tripId" class="bundle-trip">
          <text>#{{ t.tripId }} {{ t.startPoint }} → {{ t.endPoint }}</text>
        </view>
        <button
          size="mini"
          type="primary"
          class="bundle-btn"
          :disabled="!canAcceptMore"
          @click="onAcceptBundle(bundle)"
        >一键接 {{ bundle.trips.length }} 单</button>
      </view>
    </view>

    <view class="card">
      <view class="card-head">
        <text class="card-title">待接订单</text>
        <button size="mini" @click="loadPending">刷新</button>
      </view>

      <view v-if="!online" class="empty">请先上线开始接单</view>
      <view v-else-if="pending.length === 0" class="empty">暂无新订单，等待派单中...</view>

      <view v-for="group in pendingGroups" :key="group.key" class="order-card">
        <view v-if="group.poolId" class="pool-badge">拼车池 · {{ group.trips.length }} 单</view>
        <view v-for="item in group.trips" :key="item.id" class="order-trip-block">
          <view class="order-top">
            <text class="order-id">#{{ item.id }}</text>
            <text class="order-price">¥{{ item.estimatedAmount }}</text>
          </view>
          <view class="order-route">
            <text>🟢 {{ item.startPoint }}</text>
            <text>🔴 {{ item.endPoint }}</text>
          </view>
        </view>
        <view class="order-foot">
          <text>{{ group.trips.length > 1 ? '一键接全部' : formatDistance(group.trips[0].distanceMeters) }}</text>
          <button
            size="mini"
            type="primary"
            :disabled="!canAcceptMore"
            class="accept-btn"
            @click="onAccept(group.trips[0].id)"
          >{{ group.trips.length > 1 ? `接 ${group.trips.length} 单` : '立即接单' }}</button>
        </view>
      </view>
    </view>

    <view class="card log-card">
      <text class="card-title">运行日志</text>
      <scroll-view scroll-y class="log-scroll">
        <text v-for="(line, i) in logs" :key="i" class="log-line">{{ line }}</text>
        <text v-if="logs.length === 0" class="log-empty">暂无日志</text>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import {
  acceptSmartBundle,
  acceptTrip,
  arriveTrip,
  completeTrip,
  listActiveTrips,
  listPendingTrips,
  listSmartBundles,
  startTrip,
  type SmartBundle,
  type Trip,
} from '@/api/driver'
import StatusTag from '@/components/StatusTag.vue'
import {
  attachSessionHooks,
  detachSessionHooks,
  isDriverOnline,
  isWsConnected,
  startDriverSession,
  stopDriverSession,
  type DriverSessionHooks,
} from '@/utils/driverSession'
import { formatDistance, driverStepIndex } from '@/utils/trip'

const MAX_ACTIVE = 3

const driverId = ref(0)
const realName = ref('')
const online = ref(false)
const wsConnected = ref(false)
const lastLocation = ref('')
const pending = ref<Trip[]>([])
const smartBundles = ref<SmartBundle[]>([])
const activeTrips = ref<Trip[]>([])
const logs = ref<string[]>([])
const todayCompleted = ref(0)
const locCountdown = ref(60)

const driverSteps = [
  { key: 'ACCEPTED', label: '接单' },
  { key: 'ARRIVED', label: '到达' },
  { key: 'IN_PROGRESS', label: '行程' },
  { key: 'COMPLETED', label: '完单' },
]

function buildSessionHooks(): DriverSessionHooks {
  return {
    onOnlineChange: (v) => { online.value = v },
    onWsChange: (v) => { wsConnected.value = v },
    onLocationReported: (loc) => {
      lastLocation.value = loc
      locCountdown.value = 60
    },
    onPendingChange: (trips) => { pending.value = trips },
    onLog: log,
    onCountdown: (s) => { locCountdown.value = s },
  }
}

const avatarText = computed(() => (realName.value[0] || '司').toUpperCase())
const canAcceptMore = computed(() => activeTrips.value.length < MAX_ACTIVE)

/** 待接单按拼车池合并展示，避免同池重复卡片 */
const pendingGroups = computed(() => {
  const groups: { key: string; poolId?: number; trips: Trip[] }[] = []
  const poolBuckets = new Map<number, Trip[]>()
  for (const t of pending.value) {
    if (t.poolId != null) {
      const list = poolBuckets.get(t.poolId) ?? []
      list.push(t)
      poolBuckets.set(t.poolId, list)
    }
  }
  const seenPools = new Set<number>()
  for (const t of pending.value) {
    if (t.poolId != null) {
      if (seenPools.has(t.poolId)) continue
      seenPools.add(t.poolId)
      groups.push({ key: `pool-${t.poolId}`, poolId: t.poolId, trips: poolBuckets.get(t.poolId) ?? [t] })
    } else {
      groups.push({ key: `solo-${t.id}`, trips: [t] })
    }
  }
  return groups
})

function stepClassFor(status: string, key: string) {
  const idx = driverStepIndex(status)
  const target = driverStepIndex(key)
  if (idx > target) return 'done'
  if (idx === target) return 'active'
  return 'pending'
}

function formatPercent(score: number) {
  return `${Math.round(score * 100)}%`
}

function log(msg: string) {
  const line = `[${new Date().toLocaleTimeString()}] ${msg}`
  logs.value = [line, ...logs.value].slice(0, 40)
}

function goAssistant() {
  uni.navigateTo({ url: '/pages/assistant/assistant' })
}

async function loadActiveTrips() {
  if (!online.value) {
    activeTrips.value = []
    return
  }
  try {
    activeTrips.value = await listActiveTrips()
  } catch {
    activeTrips.value = []
  }
}

async function refreshAfterAccept(count: number) {
  log(`接单成功，共 ${count} 单`)
  uni.vibrateShort({})
  uni.showToast({ title: `已接 ${count} 单`, icon: 'success' })
  await loadActiveTrips()
  await loadPending()
  await loadSmartBundles()
}

function loadTodayStats() {
  todayCompleted.value = Number(uni.getStorageSync('todayCompleted') || 0)
}

function incTodayCompleted() {
  todayCompleted.value += 1
  uni.setStorageSync('todayCompleted', todayCompleted.value)
  const total = Number(uni.getStorageSync('totalCompleted') || 0) + 1
  uni.setStorageSync('totalCompleted', total)
}

async function onToggleOnline(e: { detail: { value: boolean } }) {
  const wantOnline = e.detail.value
  try {
    if (wantOnline) {
      await startDriverSession(buildSessionHooks())
    } else {
      await stopDriverSession(true)
    }
  } catch (err) {
    online.value = isDriverOnline()
    uni.showToast({ title: (err as Error).message || '操作失败', icon: 'none' })
  }
}

async function loadSmartBundles() {
  if (!online.value) return
  try {
    smartBundles.value = await listSmartBundles(5)
  } catch {
    smartBundles.value = []
  }
}

async function onAcceptBundle(bundle: SmartBundle) {
  const ids = bundle.trips.map((t) => t.tripId)
  try {
    const trips = await acceptSmartBundle(ids)
    if (trips.length === 0) {
      uni.showToast({ title: '接单失败', icon: 'none' })
      return
    }
    await refreshAfterAccept(trips.length)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '接单失败', icon: 'none' })
  }
}

async function loadPending() {
  if (!online.value) {
    uni.stopPullDownRefresh()
    return
  }
  try {
    pending.value = await listPendingTrips()
  } catch {
    /* ignore */
  } finally {
    uni.stopPullDownRefresh()
  }
}

async function onAccept(tripId: number) {
  try {
    const trips = await acceptTrip(tripId)
    await refreshAfterAccept(trips.length)
  } catch (e) {
    uni.showToast({ title: (e as Error).message || '接单失败', icon: 'none' })
  }
}

async function onArrive(tripId: number) {
  await arriveTrip(tripId)
  log(`#${tripId} 已到达上车点`)
  await loadActiveTrips()
}

async function onStart(tripId: number) {
  await startTrip(tripId)
  log(`#${tripId} 行程已开始`)
  await loadActiveTrips()
}

async function onComplete(tripId: number) {
  uni.showModal({
    title: '确认完单',
    content: `确认行程 #${tripId} 乘客已送达？`,
    success: async (res) => {
      if (!res.confirm) return
      const trip = await completeTrip(tripId)
      incTodayCompleted()
      log(`完单 #${trip.id}，收入 ¥${trip.finalAmount ?? trip.estimatedAmount}`)
      uni.showToast({ title: '完单成功', icon: 'success' })
      await loadActiveTrips()
      await loadPending()
    },
  })
}

onShow(async () => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  driverId.value = Number(uni.getStorageSync('driverId') || 0)
  realName.value = String(uni.getStorageSync('realName') || '')
  online.value = isDriverOnline()
  wsConnected.value = isWsConnected()
  loadTodayStats()
  await attachSessionHooks(buildSessionHooks())
  await loadActiveTrips()
  await loadSmartBundles()
})

onHide(() => {
  detachSessionHooks()
})

onPullDownRefresh(() => {
  loadPending()
  loadSmartBundles()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 24rpx;
  background: #f5f7fa;
}
.header {
  background: linear-gradient(135deg, #389e0d, #52c41a);
  padding: 80rpx 32rpx 40rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  color: #fff;
}
.driver-info {
  display: flex;
  align-items: center;
  gap: 20rpx;
}
.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 36rpx;
  font-weight: 700;
}
.name {
  font-size: 34rpx;
  font-weight: 700;
  display: block;
}
.sub {
  font-size: 24rpx;
  opacity: 0.85;
  display: block;
  margin-top: 4rpx;
}
.online-switch {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8rpx;
}
.ai-btn {
  background: rgba(255, 255, 255, 0.25) !important;
  color: #fff !important;
  border: 1rpx solid rgba(255, 255, 255, 0.5) !important;
  font-size: 22rpx !important;
  border-radius: 999rpx !important;
  padding: 0 20rpx !important;
}
.switch-label {
  font-size: 22rpx;
  opacity: 0.8;
}
.switch-label.on {
  font-weight: 600;
  opacity: 1;
}
.stats-row {
  display: flex;
  gap: 16rpx;
  margin: -30rpx 24rpx 20rpx;
  position: relative;
  z-index: 2;
}
.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 16rpx;
  text-align: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}
.stat-val {
  font-size: 36rpx;
  font-weight: 700;
  color: #389e0d;
  display: block;
}
.stat-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}
.status-strip {
  margin: 0 24rpx 20rpx;
  background: #fff;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 22rpx;
  color: #666;
}
.strip-item {
  display: flex;
  align-items: center;
  gap: 12rpx;
  margin-bottom: 8rpx;
}
.strip-item:last-child {
  margin-bottom: 0;
}
.strip-dot {
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: #ccc;
}
.strip-dot.on {
  background: #52c41a;
}
.card {
  margin: 0 24rpx 20rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.active-trip {
  border: 2rpx solid #b7eb8f;
}
.section-title {
  margin: 0 24rpx 12rpx;
  font-size: 28rpx;
  font-weight: 600;
  color: #333;
}
.card-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}
.card-title {
  font-size: 30rpx;
  font-weight: 700;
}
.route {
  font-size: 28rpx;
  display: block;
}
.route-arrow {
  color: #ccc;
  margin: 8rpx 0;
  display: block;
}
.fare {
  font-size: 24rpx;
  color: #389e0d;
  font-weight: 600;
  margin: 12rpx 0 20rpx;
  display: block;
}
.step-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20rpx;
}
.step {
  flex: 1;
  text-align: center;
  opacity: 0.4;
}
.step.active {
  opacity: 1;
}
.step.done {
  opacity: 0.7;
}
.step-dot {
  width: 40rpx;
  height: 40rpx;
  line-height: 40rpx;
  border-radius: 50%;
  background: #f0f0f0;
  font-size: 22rpx;
  display: inline-block;
  margin-bottom: 8rpx;
}
.step.active .step-dot {
  background: #52c41a;
  color: #fff;
}
.step.done .step-dot {
  background: #d9f7be;
  color: #389e0d;
}
.step-label {
  font-size: 20rpx;
  display: block;
}
.trip-map {
  width: 100%;
  height: 280rpx;
  border-radius: 12rpx;
  margin-bottom: 16rpx;
}
.action-row {
  display: flex;
  gap: 16rpx;
}
.act {
  flex: 1;
  border-radius: 16rpx;
  font-size: 28rpx;
  border: none;
}
.act.primary {
  background: linear-gradient(90deg, #389e0d, #52c41a);
  color: #fff;
}
.act.complete {
  background: #fff7e6;
  color: #fa8c16;
}
.empty {
  text-align: center;
  color: #999;
  padding: 40rpx 0;
  font-size: 26rpx;
}
.order-card {
  background: #fafafa;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-top: 16rpx;
}
.pool-badge {
  font-size: 22rpx;
  color: #722ed1;
  font-weight: 600;
  margin-bottom: 12rpx;
  display: block;
}
.order-trip-block {
  margin-bottom: 12rpx;
  padding-bottom: 12rpx;
  border-bottom: 1rpx solid #eee;
}
.order-trip-block:last-of-type {
  border-bottom: none;
  margin-bottom: 0;
  padding-bottom: 0;
}
.order-top {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12rpx;
}
.order-id {
  font-weight: 600;
}
.order-price {
  color: #389e0d;
  font-weight: 700;
  font-size: 32rpx;
}
.order-route {
  font-size: 26rpx;
  line-height: 1.8;
  color: #333;
  display: flex;
  flex-direction: column;
}
.order-foot {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 16rpx;
  font-size: 24rpx;
  color: #999;
}
.accept-btn {
  background: #52c41a !important;
}
.log-card {
  margin-bottom: 24rpx;
}
.log-scroll {
  max-height: 240rpx;
  margin-top: 12rpx;
}
.log-line {
  display: block;
  font-size: 22rpx;
  color: #666;
  line-height: 1.6;
  font-family: monospace;
}
.log-empty {
  font-size: 24rpx;
  color: #ccc;
}
.smart-card {
  border: 2rpx solid #d9f7be;
}
.smart-hint {
  font-size: 22rpx;
  color: #999;
  display: block;
  margin-bottom: 12rpx;
}
.bundle-card {
  background: #f6ffed;
  border-radius: 16rpx;
  padding: 20rpx;
  margin-top: 16rpx;
}
.bundle-summary {
  font-size: 26rpx;
  font-weight: 600;
  display: block;
}
.bundle-fare {
  font-size: 24rpx;
  color: #389e0d;
  margin: 8rpx 0 12rpx;
  display: block;
}
.bundle-trip {
  font-size: 24rpx;
  color: #666;
  line-height: 1.6;
}
.bundle-btn {
  margin-top: 16rpx;
  background: #52c41a !important;
}
</style>
