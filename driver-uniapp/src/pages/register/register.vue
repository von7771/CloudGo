<template>
  <view class="page">
    <view class="hero">
      <text class="brand">🚕 司机工作台</text>
      <text class="slogan">注册成为司机</text>
    </view>

    <view class="card">
      <text class="card-title">司机注册</text>
      <text class="hint">注册后需上传证件，管理员审核通过方可接单</text>

      <view class="field">
        <text class="field-icon">👤</text>
        <input v-model="username" class="input" placeholder="用户名" />
      </view>
      <view class="field">
        <text class="field-icon">📛</text>
        <input v-model="realName" class="input" placeholder="真实姓名" />
      </view>
      <view class="field">
        <text class="field-icon">✨</text>
        <input v-model="nickname" class="input" placeholder="昵称（可选）" />
      </view>
      <view class="field">
        <text class="field-icon">🔒</text>
        <input v-model="password" class="input" password placeholder="密码" />
      </view>

      <button class="btn" :loading="loading" @click="onRegister">注册并登录</button>
      <text class="link" @click="goLogin">已有账号？去登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register } from '@/api/driver'

const username = ref('')
const realName = ref('')
const nickname = ref('')
const password = ref('')
const loading = ref(false)

function goLogin() {
  uni.navigateBack()
}

async function onRegister() {
  if (!username.value || !password.value || !realName.value) {
    uni.showToast({ title: '请填写必填项', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await register(username.value, password.value, realName.value, nickname.value || undefined)
    uni.setStorageSync('token', data.token)
    uni.setStorageSync('driverId', data.driverId)
    uni.setStorageSync('realName', data.realName)
    uni.setStorageSync('username', data.username)
    uni.showToast({ title: '注册成功', icon: 'success' })
    uni.switchTab({ url: '/pages/profile/profile' })
  } catch {
    /* toast in request */
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(160deg, #389e0d 0%, #95de64 45%, #f5f7fa 45%);
}
.hero {
  padding: 120rpx 48rpx 60rpx;
  color: #fff;
}
.brand {
  font-size: 48rpx;
  font-weight: 800;
  display: block;
}
.slogan {
  margin-top: 12rpx;
  font-size: 26rpx;
  opacity: 0.9;
  display: block;
}
.card {
  margin: 0 32rpx;
  background: #fff;
  border-radius: 28rpx;
  padding: 48rpx 40rpx;
}
.card-title {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}
.hint {
  font-size: 24rpx;
  color: #999;
  margin: 12rpx 0 32rpx;
  display: block;
}
.field {
  display: flex;
  align-items: center;
  background: #f5f7fa;
  border-radius: 16rpx;
  padding: 0 24rpx;
  margin-bottom: 24rpx;
}
.field-icon {
  font-size: 32rpx;
  margin-right: 16rpx;
}
.input {
  flex: 1;
  padding: 28rpx 0;
  font-size: 28rpx;
}
.btn {
  margin-top: 8rpx;
  background: linear-gradient(90deg, #389e0d, #52c41a);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  font-size: 32rpx;
}
.link {
  display: block;
  text-align: center;
  margin-top: 32rpx;
  font-size: 26rpx;
  color: #389e0d;
}
</style>
