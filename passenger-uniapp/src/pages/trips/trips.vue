<template>
  <view class="page">
    <view class="tabs">
      <text
        v-for="tab in filterTabs"
        :key="tab.key"
        :class="['tab', { active: filter === tab.key }]"
        @click="filter = tab.key"
      >{{ tab.label }}</text>
    </view>

    <scroll-view scroll-y class="list-scroll">
      <view v-if="initialLoading" class="tip">加载中...</view>
      <view v-else-if="filtered.length === 0" class="empty">
        <text class="empty-icon">📭</text>
        <text class="empty-text">暂无{{ currentTabLabel }}行程</text>
        <button size="mini" type="primary" @click="goHome">去叫车</button>
      </view>

      <view
        v-for="item in filtered"
        :key="item.id"
        class="card"
        hover-class="card-hover"
        @tap="goDetail(item.id)"
      >
        <view class="card-top">
          <text class="time">{{ formatTime(item.createdAt) }}</text>
          <StatusTag :status="item.status" />
        </view>
        <view class="route">
          <view class="route-line">
            <view class="dot green" />
            <text class="addr">{{ item.startPoint }}</text>
          </view>
          <view class="route-line">
            <view class="dot red" />
            <text class="addr">{{ item.endPoint }}</text>
          </view>
        </view>
        <view class="card-foot">
          <text class="price">¥{{ item.finalAmount ?? item.estimatedAmount }}</text>
          <text class="dist">{{ formatDistance(item.distanceMeters) }}</text>
          <text v-if="item.passengerRating" class="rating">{{ item.passengerRating }}★ 已评</text>
          <text class="arrow">›</text>
        </view>
      </view>
    </scroll-view>

    <CustomTabBar current="trips" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import { listTrips, type Trip } from '@/api/passenger'
import CustomTabBar from '@/components/CustomTabBar.vue'
import StatusTag from '@/components/StatusTag.vue'
import { formatDistance, isActiveTrip } from '@/utils/trip'

const trips = ref<Trip[]>([])
const initialLoading = ref(false)
const refreshing = ref(false)
const filter = ref<'all' | 'active' | 'done'>('all')

const filterTabs = [
  { key: 'all' as const, label: '全部' },
  { key: 'active' as const, label: '进行中' },
  { key: 'done' as const, label: '已完成' },
]

const filtered = computed(() => {
  if (filter.value === 'active') return trips.value.filter((t) => isActiveTrip(t.status))
  if (filter.value === 'done') return trips.value.filter((t) => ['COMPLETED', 'CANCELLED'].includes(t.status))
  return trips.value
})

const currentTabLabel = computed(() => filterTabs.find((t) => t.key === filter.value)?.label || '')

const hasActiveTrips = computed(() => trips.value.some((t) => isActiveTrip(t.status)))

let pollActive = false
let pollTimer: ReturnType<typeof setTimeout> | null = null
let loadInFlight = false
let navigating = false

function stopPoll() {
  pollActive = false
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
}

function schedulePoll() {
  if (!pollActive || !hasActiveTrips.value) return
  if (pollTimer) {
    clearTimeout(pollTimer)
    pollTimer = null
  }
  pollTimer = setTimeout(async () => {
    await load(false)
    schedulePoll()
  }, 10000)
}

function formatTime(iso: string) {
  if (!iso) return ''
  return iso.replace('T', ' ').slice(0, 16)
}

function goHome() {
  stopPoll()
  uni.reLaunch({ url: '/pages/home/home' })
}

function goDetail(id: number) {
  if (navigating) return
  navigating = true
  stopPoll()
  uni.navigateTo({
    url: `/pages/trip/detail?id=${id}`,
    complete: () => {
      navigating = false
    },
    fail: () => {
      navigating = false
      uni.showToast({ title: '打开详情失败', icon: 'none' })
      pollActive = true
      schedulePoll()
    },
  })
}

async function load(showInitial = true) {
  if (loadInFlight) return
  loadInFlight = true
  if (showInitial && trips.value.length === 0) initialLoading.value = true
  try {
    trips.value = await listTrips()
  } catch {
    /* keep existing list on silent refresh failure */
  } finally {
    initialLoading.value = false
    refreshing.value = false
    loadInFlight = false
    uni.stopPullDownRefresh()
    if (pollActive && hasActiveTrips.value) {
      schedulePoll()
    }
  }
}

onShow(() => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  navigating = false
  load(trips.value.length === 0)
  stopPoll()
  pollActive = true
})

onHide(stopPoll)
onUnload(stopPoll)

onPullDownRefresh(() => {
  refreshing.value = true
  load(false)
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}
.tabs {
  flex-shrink: 0;
  display: flex;
  background: #fff;
  border-radius: 16rpx;
  padding: 8rpx;
  margin: 24rpx 24rpx 16rpx;
}
.tab {
  flex: 1;
  text-align: center;
  padding: 16rpx 0;
  font-size: 28rpx;
  color: #888;
  border-radius: 12rpx;
}
.tab.active {
  background: #1677ff;
  color: #fff;
  font-weight: 600;
}
.list-scroll {
  flex: 1;
  height: 0;
  padding: 0 24rpx 130rpx;
}
.tip,
.empty {
  text-align: center;
  padding: 100rpx 0;
  color: #888;
}
.empty-icon {
  font-size: 80rpx;
  display: block;
  margin-bottom: 16rpx;
}
.empty-text {
  display: block;
  margin-bottom: 24rpx;
}
.card {
  display: block;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
.card-hover {
  opacity: 0.85;
}
.card-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}
.time {
  font-size: 24rpx;
  color: #999;
}
.route-line {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
}
.dot {
  width: 14rpx;
  height: 14rpx;
  border-radius: 50%;
  margin-right: 16rpx;
}
.dot.green { background: #52c41a; }
.dot.red { background: #ff4d4f; }
.addr {
  font-size: 28rpx;
  flex: 1;
}
.card-foot {
  display: flex;
  align-items: center;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1px solid #f5f5f5;
  gap: 16rpx;
}
.price {
  font-size: 32rpx;
  font-weight: 700;
  color: #1677ff;
}
.dist {
  font-size: 24rpx;
  color: #999;
}
.rating {
  font-size: 22rpx;
  color: #faad14;
}
.arrow {
  margin-left: auto;
  color: #ccc;
  font-size: 36rpx;
}
</style>
