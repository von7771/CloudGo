<template>
  <view class="page">
    <view class="header">
      <text class="title">拼车 AI 助手</text>
      <text class="sub">查规则、查行程、拼车咨询</text>
    </view>

    <scroll-view scroll-y class="msg-scroll" :scroll-into-view="scrollInto" :scroll-with-animation="true">
      <view class="msg-list">
        <view v-for="(m, i) in messages" :key="i" :id="'msg-' + i" :class="['msg', m.role]">
          <text>{{ m.text }}</text>
        </view>
        <view v-if="loading" class="msg assistant"><text>思考中…</text></view>
      </view>
    </scroll-view>

    <view class="composer">
      <scroll-view scroll-x class="chips-scroll" :show-scrollbar="false">
        <view class="chips">
          <text v-for="c in quickChips" :key="c" class="chip" @tap="sendQuick(c)">{{ c }}</text>
        </view>
      </scroll-view>
      <view class="input-row">
        <input
          v-model="input"
          class="input"
          placeholder="输入问题，如：拼车怎么用？"
          confirm-type="send"
          :disabled="loading"
          @confirm="send"
        />
        <button class="send-btn" :disabled="loading || !input.trim()" @tap="send">发送</button>
      </view>
    </view>

    <CustomTabBar current="assistant" />
  </view>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { sendChat } from '@/api/assistant'
import CustomTabBar from '@/components/CustomTabBar.vue'

const input = ref('')
const loading = ref(false)
const sessionId = ref('')
const scrollInto = ref('')
const messages = ref<{ role: 'user' | 'assistant'; text: string }[]>([
  { role: 'assistant', text: '你好，我是拼车 AI 助手。可帮你查规则、查行程、拼车咨询。' },
])

const quickChips = ['拼车怎么用', '查我的行程', '费用怎么算', '帮助']

watch(messages, () => {
  scrollInto.value = 'msg-' + (messages.value.length - 1)
}, { deep: true })

onShow(() => {
  if (!uni.getStorageSync('token')) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})

async function sendQuick(text: string) {
  input.value = text
  await send()
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  input.value = ''
  messages.value.push({ role: 'user', text })
  loading.value = true
  try {
    const res = await sendChat(text, { sessionId: sessionId.value, role: 'PASSENGER' })
    sessionId.value = res.sessionId
    messages.value.push({ role: 'assistant', text: res.reply })
  } catch (e) {
    messages.value.push({ role: 'assistant', text: (e as Error).message || '请求失败' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f7fa;
}
.header {
  flex-shrink: 0;
  background: linear-gradient(135deg, #1677ff, #4096ff);
  padding: 80rpx 32rpx 28rpx;
  color: #fff;
}
.title {
  font-size: 36rpx;
  font-weight: 700;
  display: block;
}
.sub {
  font-size: 24rpx;
  opacity: 0.85;
  margin-top: 8rpx;
  display: block;
}
.msg-scroll {
  flex: 1;
  height: 0;
  width: 100%;
}
.msg-list {
  padding: 24rpx;
  padding-bottom: 32rpx;
}
.msg {
  margin-bottom: 20rpx;
  padding: 20rpx 24rpx;
  border-radius: 16rpx;
  font-size: 28rpx;
  line-height: 1.6;
  max-width: 88%;
}
.msg.user {
  background: #e6f4ff;
  margin-left: auto;
}
.msg.assistant {
  background: #fff;
  box-shadow: 0 2rpx 8rpx rgba(0, 0, 0, 0.04);
}
.composer {
  flex-shrink: 0;
  background: #fff;
  border-top: 1rpx solid #e8e8e8;
  padding: 12rpx 24rpx 16rpx;
  padding-bottom: calc(120rpx + env(safe-area-inset-bottom));
  box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, 0.06);
}
.chips-scroll {
  white-space: nowrap;
  margin-bottom: 12rpx;
}
.chips {
  display: inline-flex;
  gap: 12rpx;
}
.chip {
  display: inline-block;
  background: #f0f5ff;
  color: #1677ff;
  font-size: 24rpx;
  padding: 10rpx 20rpx;
  border-radius: 999rpx;
}
.input-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.input {
  flex: 1;
  height: 72rpx;
  line-height: 72rpx;
  background: #f5f5f5;
  border-radius: 36rpx;
  padding: 0 28rpx;
  font-size: 28rpx;
}
.send-btn {
  flex-shrink: 0;
  height: 72rpx;
  line-height: 72rpx;
  background: #1677ff;
  color: #fff;
  font-size: 28rpx;
  border-radius: 36rpx;
  border: none;
  padding: 0 36rpx;
  margin: 0;
}
.send-btn[disabled] {
  opacity: 0.5;
}
</style>
