<template>
  <view class="page">
    <view class="hero">
      <text class="brand">🚕 拼车出行</text>
      <text class="slogan">安全 · 便捷 · 智能调价</text>
    </view>

    <view class="card">
      <text class="card-title">欢迎回来</text>
      <text class="hint">演示账号 passenger1 / 123456</text>

      <view class="field">
        <text class="field-icon">👤</text>
        <input v-model="username" class="input" placeholder="用户名" />
      </view>
      <view class="field">
        <text class="field-icon">🔒</text>
        <input v-model="password" class="input" password placeholder="密码" />
      </view>

      <button class="btn" :loading="loading" @click="onLogin">登录</button>

      <text class="register-link" @click="goRegister">没有账号？立即注册</text>

      <view class="tips">
        <text>· passenger2 余额较低，可用于测试余额不足</text>
        <text>· 登录后可查看天气动态调价</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { login } from '@/api/passenger'

const username = ref('passenger1')
const password = ref('123456')
const loading = ref(false)

function goRegister() {
  uni.navigateTo({ url: '/pages/register/register' })
}

async function onLogin() {
  loading.value = true
  try {
    const data = await login(username.value, password.value)
    uni.setStorageSync('token', data.token)
    uni.setStorageSync('passengerId', data.passengerId)
    uni.setStorageSync('username', data.username)
    uni.showToast({ title: '登录成功', icon: 'success' })
    uni.reLaunch({ url: '/pages/home/home' })
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
  background: linear-gradient(160deg, #1677ff 0%, #69b1ff 45%, #f5f7fa 45%);
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
  box-shadow: 0 16rpx 48rpx rgba(22, 119, 255, 0.12);
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
  background: linear-gradient(90deg, #1677ff, #4096ff);
  color: #fff;
  border: none;
  border-radius: 16rpx;
  font-size: 32rpx;
}
.register-link {
  display: block;
  text-align: center;
  margin-top: 24rpx;
  font-size: 26rpx;
  color: #1677ff;
}
.tips {
  margin-top: 32rpx;
  font-size: 22rpx;
  color: #999;
  line-height: 1.8;
  display: flex;
  flex-direction: column;
}
</style>
