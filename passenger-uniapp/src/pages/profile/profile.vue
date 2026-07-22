<template>
  <view class="page">
    <view class="profile-header">
      <view class="avatar-wrap" @click="changeAvatar">
        <image v-if="avatarPath" class="avatar-img" :src="avatarPath" mode="aspectFill" />
        <view v-else class="avatar">{{ avatarText }}</view>
        <text class="avatar-tip">点击更换</text>
      </view>
      <text class="name">{{ displayName }}</text>
      <text class="uid">乘客 ID · {{ passengerId }}</text>
      <text v-if="profile" class="balance">余额 ¥{{ profile.balance?.toFixed(2) ?? '—' }} · 信用 {{ profile.creditScore ?? '—' }}</text>
    </view>

    <view class="edit-card">
      <text class="edit-label">昵称</text>
      <view class="edit-row">
        <input v-model="nicknameInput" class="edit-input" placeholder="设置昵称" />
        <button size="mini" class="save-btn" @click="saveNickname">保存</button>
      </view>
      <text class="edit-hint">用户名：{{ profile?.username || username }}</text>
    </view>

    <view class="stats-card">
      <view class="stat">
        <text class="stat-val">{{ stats.total }}</text>
        <text class="stat-label">总行程</text>
      </view>
      <view class="stat">
        <text class="stat-val">{{ stats.completed }}</text>
        <text class="stat-label">已完成</text>
      </view>
      <view class="stat">
        <text class="stat-val">{{ stats.active }}</text>
        <text class="stat-label">进行中</text>
      </view>
    </view>

    <view class="menu">
      <view class="menu-item" @click="goTrips">
        <text class="menu-icon">📋</text>
        <text class="menu-text">我的行程</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goHome">
        <text class="menu-icon">🚗</text>
        <text class="menu-text">立即叫车</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <button class="logout" @click="onLogout">退出登录</button>
    <CustomTabBar current="profile" />
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import {
  downloadAvatar,
  getProfile,
  listTrips,
  updateProfile,
  uploadAvatar,
  type UserProfile,
} from '@/api/passenger'
import CustomTabBar from '@/components/CustomTabBar.vue'
import { isActiveTrip } from '@/utils/trip'

const username = ref('')
const passengerId = ref(0)
const profile = ref<UserProfile | null>(null)
const nicknameInput = ref('')
const avatarPath = ref('')
const stats = ref({ total: 0, completed: 0, active: 0 })

const displayName = computed(() => profile.value?.nickname || username.value || '乘客')
const avatarText = computed(() => (displayName.value[0] || '乘').toUpperCase())

function goTrips() {
  uni.reLaunch({ url: '/pages/trips/trips' })
}

function goHome() {
  uni.reLaunch({ url: '/pages/home/home' })
}

async function loadProfile() {
  try {
    profile.value = await getProfile()
    nicknameInput.value = profile.value.nickname || profile.value.username
    if (profile.value.avatarUrl) {
      try {
        avatarPath.value = await downloadAvatar()
      } catch {
        avatarPath.value = ''
      }
    } else {
      avatarPath.value = ''
    }
  } catch {
    profile.value = null
  }
}

async function saveNickname() {
  if (!nicknameInput.value.trim()) {
    uni.showToast({ title: '昵称不能为空', icon: 'none' })
    return
  }
  try {
    profile.value = await updateProfile(nicknameInput.value.trim())
    uni.setStorageSync('username', profile.value.nickname || profile.value.username)
    username.value = String(uni.getStorageSync('username'))
    uni.showToast({ title: '已保存', icon: 'success' })
  } catch {
    /* handled */
  }
}

function changeAvatar() {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      uni.showLoading({ title: '上传中' })
      try {
        profile.value = await uploadAvatar(res.tempFilePaths[0])
        avatarPath.value = await downloadAvatar()
        uni.showToast({ title: '头像已更新', icon: 'success' })
      } catch (e) {
        uni.showToast({ title: (e as Error).message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
  })
}

async function loadStats() {
  try {
    const trips = await listTrips()
    stats.value = {
      total: trips.length,
      completed: trips.filter((t) => t.status === 'COMPLETED').length,
      active: trips.filter((t) => isActiveTrip(t.status)).length,
    }
  } catch {
    stats.value = { total: 0, completed: 0, active: 0 }
  }
}

function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '确定退出当前账号？',
    success: (res) => {
      if (!res.confirm) return
      uni.clearStorageSync()
      uni.reLaunch({ url: '/pages/login/login' })
    },
  })
}

onShow(() => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  username.value = String(uni.getStorageSync('username') || '乘客')
  passengerId.value = Number(uni.getStorageSync('passengerId') || 0)
  loadProfile()
  loadStats()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 130rpx;
  background: #f5f7fa;
}
.profile-header {
  background: linear-gradient(135deg, #1677ff, #4096ff);
  padding: 80rpx 32rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
}
.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avatar,
.avatar-img {
  width: 120rpx;
  height: 120rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.25);
}
.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48rpx;
  font-weight: 700;
}
.avatar-tip {
  font-size: 22rpx;
  opacity: 0.85;
  margin-top: 8rpx;
}
.name {
  margin-top: 20rpx;
  font-size: 36rpx;
  font-weight: 700;
}
.uid,
.balance {
  margin-top: 8rpx;
  font-size: 24rpx;
  opacity: 0.85;
}
.edit-card {
  margin: -40rpx 24rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
  padding: 28rpx;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}
.edit-label {
  font-size: 26rpx;
  color: #666;
  display: block;
  margin-bottom: 12rpx;
}
.edit-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.edit-input {
  flex: 1;
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.save-btn {
  background: #1677ff;
  color: #fff;
  border: none;
}
.edit-hint {
  font-size: 22rpx;
  color: #999;
  margin-top: 12rpx;
  display: block;
}
.stats-card {
  margin: 0 24rpx 24rpx;
  background: #fff;
  border-radius: 20rpx;
  display: flex;
  padding: 32rpx 0;
  box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.06);
}
.stat {
  flex: 1;
  text-align: center;
}
.stat-val {
  font-size: 40rpx;
  font-weight: 700;
  color: #1677ff;
  display: block;
}
.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}
.menu {
  margin: 0 24rpx;
  background: #fff;
  border-radius: 20rpx;
  overflow: hidden;
}
.menu-item {
  display: flex;
  align-items: center;
  padding: 32rpx 28rpx;
  border-bottom: 1px solid #f5f5f5;
}
.menu-item:last-child {
  border-bottom: none;
}
.menu-icon {
  font-size: 36rpx;
  margin-right: 20rpx;
}
.menu-text {
  flex: 1;
  font-size: 30rpx;
}
.menu-arrow {
  color: #ccc;
  font-size: 36rpx;
}
.logout {
  margin: 32rpx 24rpx 0;
  background: #fff;
  color: #ff4d4f;
  border: none;
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
