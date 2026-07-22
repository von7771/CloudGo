<template>
  <view class="page">
    <view class="header">
      <view class="user-row">
        <view class="avatar">{{ avatarText }}</view>
        <view>
          <text class="greet">Hi，{{ username }}</text>
          <text class="sub">今天想去哪里？</text>
        </view>
      </view>
    </view>

    <view v-if="activeTrip" class="banner" @click="goDetail(activeTrip.id)">
      <view class="banner-left">
        <text class="banner-title">进行中的行程</text>
        <text class="banner-desc">#{{ activeTrip.id }} · {{ statusMeta(activeTrip.status).label }}</text>
      </view>
      <text class="banner-link">追踪 ›</text>
    </view>

    <view class="card form-card">
      <view class="addr-row">
        <view class="dot green" />
        <input v-model="startPoint" class="addr-input" placeholder="输入起点" />
      </view>
      <view class="divider" />
      <view class="addr-row">
        <view class="dot red" />
        <input v-model="endPoint" class="addr-input" placeholder="输入终点" />
      </view>

      <scroll-view scroll-x class="chips">
        <text
          v-for="item in presets"
          :key="item.label"
          class="chip"
          @click="applyPreset(item)"
        >{{ item.label }}</text>
      </scroll-view>

      <view class="mode-row">
        <text
          class="mode-btn"
          :class="{ active: tripMode === 'SOLO' }"
          @click="tripMode = 'SOLO'"
        >独享</text>
        <text
          class="mode-btn carpool"
          :class="{ active: tripMode === 'CARPOOL' }"
          @click="tripMode = 'CARPOOL'"
        >拼车 · 85折</text>
      </view>

      <view class="btn-row">
        <button class="btn-outline" :loading="estimating" @click="onEstimate">路线估价</button>
        <button class="btn-primary" :loading="loading" @click="onCreate">
          立即叫车
        </button>
      </view>
    </view>

    <view v-if="routePreview" class="card quote-card">
      <view class="quote-head">
        <text class="quote-title">费用预估</text>
        <text class="quote-price">¥{{ routePreview.estimatedFare.toFixed(2) }}</text>
      </view>
      <view class="quote-grid">
        <view class="quote-item">
          <text class="qi-val">{{ formatDistance(routePreview.distanceMeters) }}</text>
          <text class="qi-label">里程</text>
        </view>
        <view class="quote-item">
          <text class="qi-val">{{ formatDuration(routePreview.durationSeconds) }}</text>
          <text class="qi-label">预计时长</text>
        </view>
        <view class="quote-item">
          <text class="qi-val">¥{{ routePreview.baseFare.toFixed(0) }}</text>
          <text class="qi-label">基础价</text>
        </view>
      </view>
      <view v-if="routePreview.weatherMain" class="weather-row">
        <text class="weather-icon">{{ weatherEmoji(routePreview.weatherMain) }}</text>
        <view class="weather-text">
          <text>{{ routePreview.weatherDescription || routePreview.weatherMain }}</text>
          <text v-if="routePreview.temperatureCelsius != null" class="temp">
            {{ routePreview.temperatureCelsius.toFixed(0) }}°C
          </text>
        </view>
        <view class="weather-fee">
          <text v-if="routePreview.weatherSurcharge > 0" class="surcharge">
            +¥{{ routePreview.weatherSurcharge.toFixed(2) }}
          </text>
          <text class="multiplier">×{{ routePreview.weatherMultiplier }}</text>
        </view>
      </view>
    </view>

    <map
      v-if="showMap"
      :key="mapKey"
      class="map-preview"
      :latitude="mapCenter.latitude"
      :longitude="mapCenter.longitude"
      :scale="12"
      :markers="markers"
      :polyline="polyline"
      enable-scroll
      show-location
    />

    <CustomTabBar current="home" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getRoute, type RoutePreview } from '@/api/map'
import { createTrip, listTrips, type Trip } from '@/api/passenger'
import CustomTabBar from '@/components/CustomTabBar.vue'
import {
  formatDistance,
  formatDuration,
  isActiveTrip,
  parseLngLat,
  statusMeta,
  weatherEmoji,
} from '@/utils/trip'
import { buildDrivingPolyline, centerOfPoints } from '@/utils/map'

const username = ref(String(uni.getStorageSync('username') || '乘客'))
const startPoint = ref('广州大学城')
const endPoint = ref('广州南站')
const loading = ref(false)
const estimating = ref(false)
const routePreview = ref<RoutePreview | null>(null)
const activeTrip = ref<Trip | null>(null)
const tripMode = ref<'SOLO' | 'CARPOOL'>('SOLO')
const mapCenter = ref({ latitude: 23.04, longitude: 113.33 })
const markers = ref<UniApp.MapMarker[]>([])
const polyline = ref<UniApp.MapPolyline[]>([])
const mapKey = ref(0)

const presets = [
  { label: '大学城→南站', start: '广州大学城', end: '广州南站' },
  { label: '天河→白云机场', start: '广州天河城', end: '广州白云国际机场' },
  { label: '珠江新城→北京路', start: '广州珠江新城', end: '广州北京路' },
]

const showMap = computed(
  () =>
    routePreview.value != null &&
    (markers.value.length > 0 || polyline.value.length > 0),
)
const avatarText = computed(() => (username.value[0] || '乘').toUpperCase())

function clearMap() {
  markers.value = []
  polyline.value = []
}

function applyPreset(item: (typeof presets)[0]) {
  startPoint.value = item.start
  endPoint.value = item.end
  routePreview.value = null
  clearMap()
}

function goDetail(id: number) {
  uni.navigateTo({ url: `/pages/trip/detail?id=${id}` })
}

function buildMapFromRoute(route: RoutePreview) {
  let start = parseLngLat(route.originLocation)
  let end = parseLngLat(route.destinationLocation)
  const pts = route.routePoints || []
  if (!start && pts.length > 0) start = pts[0]
  if (!end && pts.length > 0) end = pts[pts.length - 1]
  const list: UniApp.MapMarker[] = []
  if (start) list.push({ id: 1, ...start, title: '起点', width: 28, height: 28 })
  if (end) list.push({ id: 2, ...end, title: '终点', width: 28, height: 28 })
  markers.value = [...list]
  polyline.value = buildDrivingPolyline(pts, start, end)
  const center = centerOfPoints(pts) ?? (start && end
    ? { latitude: (start.latitude + end.latitude) / 2, longitude: (start.longitude + end.longitude) / 2 }
    : start ?? end)
  if (center) mapCenter.value = { ...center }
  mapKey.value += 1
}

async function onEstimate() {
  if (!startPoint.value || !endPoint.value) {
    uni.showToast({ title: '请填写起终点', icon: 'none' })
    return
  }
  estimating.value = true
  try {
    routePreview.value = await getRoute(startPoint.value, endPoint.value)
    buildMapFromRoute(routePreview.value)
  } catch (e) {
    const msg = e instanceof Error ? e.message : '估价失败'
    uni.showToast({ title: msg.slice(0, 40) || '估价失败', icon: 'none', duration: 3000 })
  } finally {
    estimating.value = false
  }
}

async function onCreate() {
  if (!startPoint.value || !endPoint.value) {
    uni.showToast({ title: '请填写起终点', icon: 'none' })
    return
  }
  loading.value = true
  try {
    if (!routePreview.value) {
      uni.showToast({ title: '正在规划路线...', icon: 'none' })
    }
    const trip = await createTrip(startPoint.value, endPoint.value, tripMode.value)
    const modeLabel = tripMode.value === 'CARPOOL' ? '拼车发单成功' : '发单成功'
    if (routePreview.value) {
      uni.setStorageSync(`trip_route_${trip.id}`, JSON.stringify(routePreview.value))
    }
    uni.showToast({ title: modeLabel, icon: 'success', duration: 1200 })
    clearMap()
    setTimeout(() => {
      uni.redirectTo({ url: `/pages/trip/detail?id=${trip.id}` })
    }, 300)
  } finally {
    loading.value = false
  }
}

async function loadActiveTrip() {
  try {
    const trips = await listTrips()
    activeTrip.value = trips.find((t) => isActiveTrip(t.status)) || null
  } catch {
    activeTrip.value = null
  }
}

onShow(() => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  username.value = String(uni.getStorageSync('username') || '乘客')
  loadActiveTrip()
  if (routePreview.value && markers.value.length === 0) {
    buildMapFromRoute(routePreview.value)
  }
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 130rpx;
  background: #f5f7fa;
}
.header {
  background: linear-gradient(135deg, #1677ff, #4096ff);
  padding: 80rpx 32rpx 40rpx;
  color: #fff;
}
.user-row {
  display: flex;
  align-items: center;
  gap: 24rpx;
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
.greet {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}
.sub {
  font-size: 24rpx;
  opacity: 0.85;
  margin-top: 6rpx;
  display: block;
}
.banner {
  margin: -20rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx 28rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
  position: relative;
  z-index: 2;
}
.banner-title {
  font-weight: 600;
  display: block;
  font-size: 28rpx;
}
.banner-desc {
  font-size: 24rpx;
  color: #1677ff;
  margin-top: 6rpx;
  display: block;
}
.banner-link {
  color: #1677ff;
  font-size: 28rpx;
}
.map-preview {
  width: calc(100% - 48rpx);
  height: 400rpx;
  margin: 0 24rpx 24rpx;
  border-radius: 20rpx;
  overflow: hidden;
}
.form-card {
  margin: 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 8rpx 24rpx 24rpx;
  box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.04);
}
.addr-row {
  display: flex;
  align-items: center;
  padding: 20rpx 0;
}
.dot {
  width: 16rpx;
  height: 16rpx;
  border-radius: 50%;
  margin-right: 20rpx;
}
.dot.green { background: #52c41a; }
.dot.red { background: #ff4d4f; }
.addr-input {
  flex: 1;
  font-size: 30rpx;
}
.divider {
  height: 1px;
  background: #f0f0f0;
  margin-left: 36rpx;
}
.chips {
  white-space: nowrap;
  margin: 16rpx 0 24rpx;
}
.chip {
  display: inline-block;
  background: #f0f5ff;
  color: #1677ff;
  font-size: 24rpx;
  padding: 12rpx 24rpx;
  border-radius: 999rpx;
  margin-right: 16rpx;
}
.mode-row {
  display: flex;
  gap: 16rpx;
  margin-bottom: 24rpx;
}
.mode-btn {
  flex: 1;
  text-align: center;
  padding: 20rpx 0;
  border-radius: 16rpx;
  font-size: 28rpx;
  background: #f5f7fa;
  color: #666;
}
.mode-btn.active {
  background: #e6f4ff;
  color: #1677ff;
  font-weight: 600;
}
.mode-btn.carpool.active {
  background: #f9f0ff;
  color: #722ed1;
}
.btn-row {
  display: flex;
  gap: 16rpx;
}
.btn-outline,
.btn-primary {
  flex: 1;
  font-size: 28rpx;
  border-radius: 16rpx;
  border: none;
}
.btn-outline {
  background: #f5f7fa;
  color: #1677ff;
}
.btn-primary {
  background: linear-gradient(90deg, #1677ff, #4096ff);
  color: #fff;
}
.btn-primary[disabled] {
  opacity: 0.5;
}
.quote-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
}
.quote-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 24rpx;
}
.quote-title {
  font-size: 28rpx;
  color: #666;
}
.quote-price {
  font-size: 48rpx;
  font-weight: 800;
  color: #1677ff;
}
.quote-grid {
  display: flex;
  justify-content: space-around;
  padding: 16rpx 0;
  border-top: 1px solid #f5f5f5;
  border-bottom: 1px solid #f5f5f5;
}
.quote-item {
  text-align: center;
}
.qi-val {
  font-size: 30rpx;
  font-weight: 600;
  display: block;
}
.qi-label {
  font-size: 22rpx;
  color: #999;
  margin-top: 6rpx;
  display: block;
}
.weather-row {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  gap: 16rpx;
}
.weather-icon {
  font-size: 40rpx;
}
.weather-text {
  flex: 1;
  font-size: 24rpx;
  color: #666;
  display: flex;
  flex-direction: column;
}
.temp {
  font-size: 22rpx;
  color: #999;
  margin-top: 4rpx;
}
.weather-fee {
  text-align: right;
}
.surcharge {
  display: block;
  color: #fa8c16;
  font-size: 26rpx;
  font-weight: 600;
}
.multiplier {
  font-size: 22rpx;
  color: #999;
}
</style>
