<template>
  <view class="page">
    <view class="profile-header">
      <view class="avatar-wrap" @click="changeAvatar">
        <image v-if="avatarPath" class="avatar-img" :src="avatarPath" mode="aspectFill" />
        <view v-else class="avatar">{{ avatarText }}</view>
        <text class="avatar-tip">点击更换</text>
      </view>
      <text class="name">{{ displayName }}</text>
      <text class="uid">司机 ID · #{{ driverId }}</text>
    </view>

    <view class="edit-card">
      <text class="edit-label">昵称</text>
      <view class="edit-row">
        <input v-model="nicknameInput" class="edit-input" placeholder="设置昵称" />
        <button size="mini" class="save-btn" @click="saveProfile">保存</button>
      </view>
      <text class="edit-label mt">真实姓名</text>
      <input v-model="realNameInput" class="edit-input full" placeholder="真实姓名" />
    </view>

    <view class="stats-card">
      <view class="stat">
        <text class="stat-val">{{ todayCompleted }}</text>
        <text class="stat-label">今日完单</text>
      </view>
      <view class="stat">
        <text class="stat-val">{{ totalCompleted }}</text>
        <text class="stat-label">累计完单</text>
      </view>
    </view>

    <view class="menu">
      <view class="menu-item" @click="goAssistant">
        <text class="menu-icon">🤖</text>
        <text class="menu-text">AI 助手</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goWorkbench">
        <text class="menu-icon">🚕</text>
        <text class="menu-text">返回工作台</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="uploadDoc('license')">
        <text class="menu-icon">📄</text>
        <text class="menu-text">上传驾驶证 {{ docStatus.licenseUploaded ? '✓' : '' }}</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="uploadDoc('id_card')">
        <text class="menu-icon">🪪</text>
        <text class="menu-text">上传身份证 {{ docStatus.idCardUploaded ? '✓' : '' }}</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="clearStats">
        <text class="menu-icon">🔄</text>
        <text class="menu-text">重置今日统计</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <view class="tips">
      <text class="tips-title">审核状态 · {{ docStatus.auditStatus || '未知' }}</text>
      <text>· PENDING 可上传证件，审核通过后可上线接单</text>
      <text>· 上线后每 60 秒自动上报位置</text>
      <text>· 实时推送 + 10 秒轮询，任一通道均可收到新单</text>
    </view>

    <button class="logout" @click="onLogout">退出登录</button>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getDocumentStatus, getProfile, updateProfile, uploadAvatar, uploadDocument, downloadAvatar } from '@/api/driver'
import { clearDriverSessionOnLogout, stopDriverSession } from '@/utils/driverSession'

const realName = ref('')
const realNameInput = ref('')
const nicknameInput = ref('')
const driverId = ref(0)
const avatarPath = ref('')
const todayCompleted = ref(0)
const totalCompleted = ref(0)
const docStatus = ref({
  auditStatus: '',
  licenseUploaded: false,
  idCardUploaded: false,
})

const displayName = computed(() => nicknameInput.value || realName.value || '司机')
const avatarText = computed(() => (displayName.value[0] || '司').toUpperCase())

function goWorkbench() {
  uni.switchTab({ url: '/pages/workbench/workbench' })
}

function goAssistant() {
  uni.navigateTo({ url: '/pages/assistant/assistant' })
}

async function loadDocStatus() {
  try {
    docStatus.value = await getDocumentStatus()
  } catch {
    // ignore
  }
}

async function loadProfile() {
  try {
    const p = await getProfile()
    nicknameInput.value = p.nickname || p.username
    realNameInput.value = p.realName || ''
    realName.value = p.realName || '司机'
    if (p.avatarUrl) {
      try {
        avatarPath.value = await downloadAvatar()
      } catch {
        avatarPath.value = ''
      }
    }
  } catch {
    // ignore
  }
}

async function saveProfile() {
  try {
    const p = await updateProfile(nicknameInput.value.trim(), realNameInput.value.trim())
    realName.value = p.realName || realNameInput.value
    uni.setStorageSync('realName', realName.value)
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
        await uploadAvatar(res.tempFilePaths[0])
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

function uploadDoc(docType: 'license' | 'id_card') {
  uni.chooseImage({
    count: 1,
    sizeType: ['compressed'],
    success: async (res) => {
      const filePath = res.tempFilePaths[0]
      uni.showLoading({ title: '上传中' })
      try {
        await uploadDocument(docType, filePath)
        uni.showToast({ title: '上传成功', icon: 'success' })
        loadDocStatus()
      } catch (e) {
        uni.showToast({ title: (e as Error).message || '上传失败', icon: 'none' })
      } finally {
        uni.hideLoading()
      }
    },
  })
}

function clearStats() {
  uni.showModal({
    title: '重置统计',
    content: '确定重置今日完单数？',
    success: (res) => {
      if (!res.confirm) return
      uni.setStorageSync('todayCompleted', 0)
      todayCompleted.value = 0
      uni.showToast({ title: '已重置', icon: 'success' })
    },
  })
}

function onLogout() {
  uni.showModal({
    title: '退出登录',
    content: '退出将停止位置上报，确定退出？',
    success: async (res) => {
      if (!res.confirm) return
      try {
        await stopDriverSession(true)
      } catch {
        /* ignore */
      }
      clearDriverSessionOnLogout()
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
  realName.value = String(uni.getStorageSync('realName') || '司机')
  realNameInput.value = realName.value
  driverId.value = Number(uni.getStorageSync('driverId') || 0)
  todayCompleted.value = Number(uni.getStorageSync('todayCompleted') || 0)
  totalCompleted.value = Number(uni.getStorageSync('totalCompleted') || 0) + todayCompleted.value
  loadProfile()
  loadDocStatus()
})
</script>

<style scoped>
.page {
  min-height: 100vh;
  padding-bottom: 24rpx;
  background: #f5f7fa;
}
.profile-header {
  background: linear-gradient(135deg, #389e0d, #52c41a);
  padding: 80rpx 32rpx 60rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  color: #fff;
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
.avatar-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
}
.avatar-tip {
  font-size: 22rpx;
  opacity: 0.85;
  margin-top: 8rpx;
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
.edit-label.mt {
  margin-top: 20rpx;
}
.edit-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.edit-input {
  background: #f5f7fa;
  border-radius: 12rpx;
  padding: 16rpx 20rpx;
  font-size: 28rpx;
}
.edit-input.full {
  width: 100%;
  box-sizing: border-box;
}
.edit-row .edit-input {
  flex: 1;
}
.save-btn {
  background: #389e0d;
  color: #fff;
  border: none;
}
.name {
  margin-top: 20rpx;
  font-size: 36rpx;
  font-weight: 700;
}
.uid {
  margin-top: 8rpx;
  font-size: 24rpx;
  opacity: 0.85;
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
  color: #389e0d;
  display: block;
}
.stat-label {
  font-size: 24rpx;
  color: #999;
  margin-top: 8rpx;
  display: block;
}
.menu {
  margin: 0 24rpx 24rpx;
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
.tips {
  margin: 0 24rpx 32rpx;
  background: #f6ffed;
  border-radius: 16rpx;
  padding: 24rpx;
  font-size: 24rpx;
  color: #666;
  line-height: 1.8;
  display: flex;
  flex-direction: column;
}
.tips-title {
  font-weight: 600;
  color: #389e0d;
  margin-bottom: 8rpx;
}
.logout {
  margin: 0 24rpx;
  background: #fff;
  color: #ff4d4f;
  border: none;
  border-radius: 16rpx;
  font-size: 30rpx;
}
</style>
