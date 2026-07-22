<template>
  <view class="tabbar">
    <view
      v-for="item in tabs"
      :key="item.path"
      class="tab"
      :class="{ active: current === item.key }"
      @click="switchTab(item.path)"
    >
      <text class="icon">{{ item.icon }}</text>
      <text class="label">{{ item.label }}</text>
      <view v-if="item.key === 'workbench' && badge > 0" class="badge">{{ badge > 9 ? '9+' : badge }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{ current: 'workbench' | 'profile'; badge?: number }>()

const tabs = [
  { key: 'workbench', label: '工作台', icon: '🚕', path: '/pages/workbench/workbench' },
  { key: 'profile', label: '我的', icon: '👤', path: '/pages/profile/profile' },
]

function switchTab(path: string) {
  uni.switchTab({ url: path })
}
</script>

<style scoped>
.tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 110rpx;
  padding-bottom: env(safe-area-inset-bottom);
  background: #fff;
  border-top: 1px solid #f0f0f0;
  display: flex;
  z-index: 99;
}
.tab {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  position: relative;
}
.tab.active {
  color: #52c41a;
}
.icon {
  font-size: 36rpx;
}
.label {
  font-size: 22rpx;
  margin-top: 4rpx;
}
.badge {
  position: absolute;
  top: 8rpx;
  right: 50%;
  margin-right: -48rpx;
  background: #ff4d4f;
  color: #fff;
  font-size: 18rpx;
  min-width: 32rpx;
  height: 32rpx;
  line-height: 32rpx;
  text-align: center;
  border-radius: 999rpx;
  padding: 0 8rpx;
}
</style>
