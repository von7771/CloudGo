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
    </view>
  </view>
</template>

<script setup lang="ts">
defineProps<{ current: 'home' | 'trips' | 'assistant' | 'profile' }>()

const tabs = [
  { key: 'home', label: '叫车', icon: '🚗', path: '/pages/home/home' },
  { key: 'trips', label: '行程', icon: '📋', path: '/pages/trips/trips' },
  { key: 'assistant', label: 'AI助手', icon: '🤖', path: '/pages/assistant/assistant' },
  { key: 'profile', label: '我的', icon: '👤', path: '/pages/profile/profile' },
]

function switchTab(path: string) {
  uni.reLaunch({ url: path })
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
}
.tab.active {
  color: #1677ff;
}
.icon {
  font-size: 36rpx;
  line-height: 1.2;
}
.label {
  font-size: 20rpx;
  margin-top: 4rpx;
}
</style>
