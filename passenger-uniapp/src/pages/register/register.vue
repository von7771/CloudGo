<template>
  <view class="page">
    <view class="hero">
      <text class="brand">🚕 拼车出行</text>
      <text class="slogan">创建乘客账号</text>
    </view>

    <view class="card">
      <text class="card-title">注册</text>

      <view class="field">
        <text class="field-icon">👤</text>
        <input v-model="username" class="input" placeholder="用户名（3-32位）" />
      </view>
      <view class="field">
        <text class="field-icon">✨</text>
        <input v-model="nickname" class="input" placeholder="昵称（可选）" />
      </view>
      <view class="field">
        <text class="field-icon">🔒</text>
        <input v-model="password" class="input" password placeholder="密码（至少6位）" />
      </view>
      <view class="field">
        <text class="field-icon">🔒</text>
        <input v-model="confirmPassword" class="input" password placeholder="确认密码" />
      </view>

      <button class="btn" :loading="loading" @click="onRegister">注册并登录</button>
      <text class="link" @click="goLogin">已有账号？去登录</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { register } from '@/api/passenger'

const username = ref('')
const nickname = ref('')
const password = ref('')
const confirmPassword = ref('')
const loading = ref(false)

function goLogin() {
  uni.navigateBack()
}

async function onRegister() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请填写用户名和密码', icon: 'none' })
    return
  }
  if (password.value !== confirmPassword.value) {
    uni.showToast({ title: '两次密码不一致', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data = await register(username.value, password.value, nickname.value || undefined)
    uni.setStorageSync('token', data.token)
    uni.setStorageSync('passengerId', data.passengerId)
    uni.setStorageSync('username', data.username)
    uni.showToast({ title: '注册成功', icon: 'success' })
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
  margin-bottom: 32rpx;
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
.link {
  display: block;
  text-align: center;
  margin-top: 32rpx;
  font-size: 26rpx;
  color: #1677ff;
}
</style>
