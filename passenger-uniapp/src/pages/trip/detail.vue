<template>
  <view class="page">
    <view v-if="pageLoading" class="loading-bar">
      <text>加载行程中…</text>
    </view>

    <view v-if="trip" class="top-card">
      <view class="top-row">
        <StatusTag :status="trip.status" />
        <text class="trip-id">#{{ trip.id }}</text>
      </view>
      <TripStepper :status="trip.status" />
    </view>

    <view v-if="trip && !showMap" class="map-placeholder" @tap="enableMap">
      <text class="map-ph-icon">🗺</text>
      <text class="map-ph-text">点击查看路线地图</text>
    </view>

    <map
      v-if="trip && showMap && mapReady"
      class="map"
      :latitude="mapCenter.latitude"
      :longitude="mapCenter.longitude"
      :scale="13"
      :markers="markers"
      :polyline="polyline"
    />

    <view v-if="trip" class="bottom-sheet">
      <view class="route-block">
        <view class="route-item">
          <view class="dot green" />
          <view>
            <text class="rlabel">起点</text>
            <text class="raddr">{{ trip.startPoint }}</text>
          </view>
        </view>
        <view class="route-item">
          <view class="dot red" />
          <view>
            <text class="rlabel">终点</text>
            <text class="raddr">{{ trip.endPoint }}</text>
          </view>
        </view>
      </view>

      <view v-if="poolInfo && trip.tripMode === 'CARPOOL'" class="pool-banner">
        <text class="pool-title">🤝 拼车 {{ poolTitle }}</text>
        <text class="pool-desc">{{ poolDesc }}</text>
        <view v-if="poolInfo.members?.length" class="pool-members">
          <view v-for="m in poolInfo.members" :key="m.tripId" class="pool-member" :class="{ self: m.isSelf }">
            <view class="pm-head">
              <text class="pm-tag">{{ m.isSelf ? '我的行程' : '拼友' }} #{{ m.tripId }}</text>
              <text class="pm-status">{{ memberStatusLabel(m.status) }}</text>
            </view>
            <text class="pm-route">🟢 {{ m.startPoint }}</text>
            <text class="pm-route">🔴 {{ m.endPoint }}</text>
          </view>
        </view>
      </view>

      <view class="info-grid">
        <view class="info-cell">
          <text class="iv">{{ formatDistance(trip.distanceMeters) }}</text>
          <text class="il">里程</text>
        </view>
        <view class="info-cell">
          <text class="iv">¥{{ trip.finalAmount ?? trip.estimatedAmount }}</text>
          <text class="il">{{ trip.finalAmount ? '实付' : '预估' }}</text>
        </view>
        <view class="info-cell">
          <text class="iv">{{ trip.driverId ? `#${trip.driverId}` : '—' }}</text>
          <text class="il">司机</text>
        </view>
      </view>

      <view v-if="driverLoc" class="driver-loc">
        <text class="dl-title">🚗 司机实时位置</text>
        <text class="dl-meta">更新于 {{ driverLoc.updatedAt }}</text>
        <button size="mini" @tap="refreshDriverLocation(true)">立即刷新</button>
      </view>

      <view class="actions">
        <button v-if="canCancel" class="act-btn warn" @tap="onCancel">取消行程</button>
        <button v-if="canRate" class="act-btn primary" @tap="showRate = true">评价司机</button>
      </view>
    </view>

    <view v-if="showRate" class="mask" @tap="showRate = false">
      <view class="rate-panel" @tap.stop>
        <text class="rate-title">为本次行程评分</text>
        <StarRating v-model="rating" />
        <button type="primary" @tap="onRate">提交评价</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onLoad, onUnload } from '@dcloudio/uni-app'
import {
  cancelTrip,
  getDriverLocation,
  getPoolStatus,
  getTrip,
  rateTrip,
  type DriverLocation,
  type PoolStatus,
  type Trip,
} from '@/api/passenger'
import { getRoute } from '@/api/map'
import StarRating from '@/components/StarRating.vue'
import StatusTag from '@/components/StatusTag.vue'
import TripStepper from '@/components/TripStepper.vue'
import { buildDrivingPolyline, centerOfPoints, type MapPoint } from '@/utils/map'
import { formatDistance, isActiveTrip, parseLngLat, statusMeta } from '@/utils/trip'

const tripId = ref(0)
const trip = ref<Trip | null>(null)
const poolInfo = ref<PoolStatus | null>(null)
const driverLoc = ref<DriverLocation | null>(null)
const mapCenter = ref({ latitude: 23.04, longitude: 113.33 })
const markers = ref<UniApp.MapMarker[]>([])
const polyline = ref<UniApp.MapPolyline[]>([])
const routePoints = ref<MapPoint[]>([])
const showRate = ref(false)
const rating = ref(5)
const showMap = ref(false)
const mapReady = ref(false)
const pageLoading = ref(false)

const STATUS_POLL_MS = 8000
const LOCATION_POLL_MS = 30000
const TERMINAL_STATUSES = ['COMPLETED', 'CANCELLED']

let statusPollActive = false
let locationPollActive = false
let statusPollTimer: ReturnType<typeof setTimeout> | null = null
let locationPollTimer: ReturnType<typeof setTimeout> | null = null
let loadTripInFlight = false
let statusRefreshing = false
let locationRefreshing = false

const trackable = ['ACCEPTED', 'ARRIVED', 'IN_PROGRESS']
const canCancel = computed(() => trip.value && ['CREATED', 'DISPATCHING', 'POOL_WAITING'].includes(trip.value.status))
const canRate = computed(() => trip.value?.status === 'COMPLETED' && !trip.value.passengerRating)

const poolTitle = computed(() => {
  if (!poolInfo.value) return ''
  return `${poolInfo.value.currentSeats}/${poolInfo.value.maxSeats} 人`
})

const poolDesc = computed(() => {
  const t = trip.value
  const p = poolInfo.value
  if (!t || !p) return ''
  if (t.status === 'COMPLETED') return '本次拼车行程已完成'
  if (t.status === 'CANCELLED') return '行程已取消'
  if (t.status === 'IN_PROGRESS') return '司机正在送你，请系好安全带'
  if (t.status === 'ARRIVED') return '司机已到达你的上车点'
  if (t.status === 'ACCEPTED') return `司机 #${t.driverId ?? p.driverId ?? '—'} 已接单，正在赶来`
  if (t.status === 'DISPATCHING') return '已派单，等待司机接单…'
  if (t.status === 'POOL_WAITING') {
    if (p.status === 'DISPATCHING') return '已派单，等待司机接单…'
    return '正在匹配拼友，满员后自动派单'
  }
  if (p.status === 'WAITING') return '正在匹配拼友，满员后自动派单'
  return '拼车进行中'
})

function memberStatusLabel(status: string) {
  return statusMeta(status).label
}

function resetPageState() {
  stopPoll()
  trip.value = null
  poolInfo.value = null
  driverLoc.value = null
  routePoints.value = []
  markers.value = []
  polyline.value = []
  showMap.value = false
  mapReady.value = false
  loadTripInFlight = false
}

function buildMap() {
  const list: UniApp.MapMarker[] = []
  const start = parseLngLat(trip.value?.startLocation ?? null)
  const end = parseLngLat(trip.value?.endLocation ?? null)
  if (start) list.push({ id: 1, ...start, title: '起点', width: 28, height: 28 })
  if (end) list.push({ id: 2, ...end, title: '终点', width: 28, height: 28 })
  if (driverLoc.value) {
    list.push({
      id: 3,
      latitude: driverLoc.value.latitude,
      longitude: driverLoc.value.longitude,
      title: '司机',
      width: 36,
      height: 36,
    })
  }
  markers.value = list
  polyline.value = buildDrivingPolyline(routePoints.value, start, end)
  const center = driverLoc.value
    ? { latitude: driverLoc.value.latitude, longitude: driverLoc.value.longitude }
    : centerOfPoints(routePoints.value) ?? start
  if (center) mapCenter.value = center
}

function loadCachedRoute() {
  const raw = uni.getStorageSync(`trip_route_${tripId.value}`)
  if (!raw) return false
  try {
    const cached = JSON.parse(String(raw)) as { routePoints?: MapPoint[] }
    if (cached.routePoints?.length) {
      routePoints.value = cached.routePoints
      return true
    }
  } catch { /* ignore */ }
  return false
}

async function loadRoutePolyline() {
  if (!trip.value?.startPoint || !trip.value?.endPoint) return
  if (loadCachedRoute()) {
    buildMap()
    return
  }
  try {
    const route = await getRoute(trip.value.startPoint, trip.value.endPoint)
    routePoints.value = route.routePoints
    buildMap()
  } catch {
    routePoints.value = []
  }
}

function enableMap() {
  showMap.value = true
  buildMap()
  setTimeout(() => { mapReady.value = true }, 300)
  if (!routePoints.value.length) {
    loadRoutePolyline()
  }
}

async function refreshPoolInfo() {
  const t = trip.value
  if (!t?.poolId || t.tripMode !== 'CARPOOL' || ['COMPLETED', 'CANCELLED'].includes(t.status)) {
    poolInfo.value = null
    return
  }
  try {
    poolInfo.value = await getPoolStatus(tripId.value)
  } catch {
    poolInfo.value = null
  }
}

async function refreshDriverLocation(showToast = false) {
  if (!trip.value || !trackable.includes(trip.value.status) || locationRefreshing) return
  locationRefreshing = true
  try {
    driverLoc.value = await getDriverLocation(tripId.value)
    if (showMap.value) buildMap()
    if (showToast) uni.showToast({ title: '位置已更新', icon: 'none' })
  } catch {
    if (showToast) uni.showToast({ title: '暂无司机位置', icon: 'none' })
  } finally {
    locationRefreshing = false
  }
}

async function refreshTripStatus() {
  if (!tripId.value || statusRefreshing) return
  statusRefreshing = true
  try {
    const updated = await getTrip(tripId.value)
    const statusChanged = trip.value?.status !== updated.status
    trip.value = updated
    if (updated.tripMode === 'CARPOOL' && isActiveTrip(updated.status)) {
      await refreshPoolInfo()
    }
    if (statusChanged && showMap.value) buildMap()
    if (TERMINAL_STATUSES.includes(updated.status)) stopPoll()
  } catch {
    /* ignore */
  } finally {
    statusRefreshing = false
  }
}

async function loadTrip() {
  if (loadTripInFlight || !tripId.value) return
  loadTripInFlight = true
  pageLoading.value = true
  try {
    trip.value = await getTrip(tripId.value)
    pageLoading.value = false
    if (trip.value?.tripMode === 'CARPOOL' && isActiveTrip(trip.value.status)) {
      void refreshPoolInfo()
    }
    if (trip.value && trackable.includes(trip.value.status)) {
      void refreshDriverLocation(false)
    }
    if (trip.value && isActiveTrip(trip.value.status)) {
      setTimeout(() => startPoll(), 400)
    }
  } catch (e) {
    pageLoading.value = false
    uni.showToast({ title: (e as Error).message || '加载失败', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 1500)
  } finally {
    loadTripInFlight = false
  }
}

function scheduleStatusPoll() {
  if (!statusPollActive) return
  statusPollTimer = setTimeout(async () => {
    await refreshTripStatus()
    scheduleStatusPoll()
  }, STATUS_POLL_MS)
}

function scheduleLocationPoll() {
  if (!locationPollActive) return
  locationPollTimer = setTimeout(async () => {
    await refreshDriverLocation(false)
    scheduleLocationPoll()
  }, LOCATION_POLL_MS)
}

function startPoll() {
  stopPoll()
  statusPollActive = true
  locationPollActive = trackable.includes(trip.value?.status ?? '')
  scheduleStatusPoll()
  if (locationPollActive) scheduleLocationPoll()
}

function stopPoll() {
  statusPollActive = false
  locationPollActive = false
  if (statusPollTimer) {
    clearTimeout(statusPollTimer)
    statusPollTimer = null
  }
  if (locationPollTimer) {
    clearTimeout(locationPollTimer)
    locationPollTimer = null
  }
}

async function onCancel() {
  uni.showModal({
    title: '确认取消',
    content: '确定要取消本次行程吗？',
    success: async (res) => {
      if (!res.confirm) return
      await cancelTrip(tripId.value)
      uni.showToast({ title: '已取消', icon: 'success' })
      await loadTrip()
    },
  })
}

async function onRate() {
  await rateTrip(tripId.value, rating.value)
  showRate.value = false
  uni.showToast({ title: '评价成功', icon: 'success' })
  await loadTrip()
}

onLoad((query) => {
  resetPageState()
  tripId.value = Number(query?.id || 0)
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  if (tripId.value) loadTrip()
})

onHide(() => {
  stopPoll()
  showMap.value = false
  mapReady.value = false
})

onUnload(resetPageState)
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 40rpx;
  background: #f5f7fa;
}
.loading-bar {
  background: #e6f4ff;
  color: #1677ff;
  text-align: center;
  padding: 16rpx;
  font-size: 26rpx;
}
.top-card {
  background: #fff;
  padding: 24rpx 28rpx;
  flex-shrink: 0;
}
.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8rpx;
}
.trip-id {
  font-size: 24rpx;
  color: #999;
}
.map-placeholder {
  height: 240rpx;
  margin: 16rpx 24rpx;
  background: linear-gradient(135deg, #e6f4ff, #f0f5ff);
  border-radius: 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.map-ph-icon {
  font-size: 56rpx;
}
.map-ph-text {
  font-size: 26rpx;
  color: #1677ff;
  margin-top: 8rpx;
}
.map {
  height: 360rpx;
  width: 100%;
  flex-shrink: 0;
}
.bottom-sheet {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  margin-top: 16rpx;
  padding: 28rpx;
  box-shadow: 0 -8rpx 32rpx rgba(0, 0, 0, 0.06);
}
.route-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16rpx;
}
.pool-banner {
  background: #f9f0ff;
  border-radius: 12rpx;
  padding: 20rpx;
  margin-bottom: 16rpx;
}
.pool-title {
  font-size: 28rpx;
  font-weight: 600;
  color: #722ed1;
  display: block;
}
.pool-desc {
  font-size: 24rpx;
  color: #666;
  margin-top: 6rpx;
  display: block;
}
.pool-members {
  margin-top: 16rpx;
}
.pool-member {
  background: #fff;
  border-radius: 10rpx;
  padding: 14rpx 16rpx;
  margin-bottom: 10rpx;
}
.pool-member.self {
  border: 1rpx solid #b37feb;
}
.pm-tag {
  font-size: 22rpx;
  color: #722ed1;
  font-weight: 600;
}
.pm-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6rpx;
}
.pm-status {
  font-size: 20rpx;
  color: #999;
}
.pm-route {
  font-size: 24rpx;
  color: #333;
  line-height: 1.6;
  display: block;
}
.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 16rpx;
  margin-top: 8rpx;
}
.dot.green { background: #52c41a; }
.dot.red { background: #ff4d4f; }
.rlabel {
  font-size: 22rpx;
  color: #999;
  display: block;
}
.raddr {
  font-size: 28rpx;
  display: block;
}
.info-grid {
  display: flex;
  margin: 20rpx 0;
  padding: 20rpx 0;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
}
.info-cell {
  flex: 1;
  text-align: center;
}
.iv {
  font-size: 30rpx;
  font-weight: 600;
  display: block;
}
.il {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}
.driver-loc {
  display: flex;
  align-items: center;
  gap: 12rpx;
  background: #f0f5ff;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  margin-bottom: 16rpx;
}
.dl-title {
  font-size: 26rpx;
  font-weight: 600;
}
.dl-meta {
  flex: 1;
  font-size: 22rpx;
  color: #888;
}
.actions {
  display: flex;
  gap: 16rpx;
  padding-bottom: 40rpx;
}
.act-btn {
  flex: 1;
  border-radius: 16rpx;
  font-size: 28rpx;
  border: none;
}
.act-btn.warn {
  background: #fff1f0;
  color: #ff4d4f;
}
.act-btn.primary {
  background: linear-gradient(90deg, #1677ff, #4096ff);
  color: #fff;
}
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: flex-end;
  z-index: 100;
}
.rate-panel {
  width: 100%;
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 40rpx;
}
.rate-title {
  font-size: 32rpx;
  font-weight: 600;
  text-align: center;
  display: block;
  margin-bottom: 16rpx;
}
</style>
