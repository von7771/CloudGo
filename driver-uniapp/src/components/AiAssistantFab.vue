<template>
  <view v-if="visible" :class="['ai-root', placement]">
    <view v-if="!open" class="fab" @click="openPanel">
      <text class="fab-icon">AI</text>
    </view>

    <view v-else class="panel">
      <view class="panel-head">
        <text class="panel-title">拼车助手</text>
        <text class="panel-close" @click="open = false">×</text>
      </view>

      <scroll-view scroll-y class="msg-scroll" :scroll-into-view="scrollInto">
        <view v-for="(m, i) in messages" :key="i" :id="'msg-' + i" :class="['msg', m.role]">
          <text>{{ m.text }}</text>
        </view>
        <view v-if="loading" class="msg assistant"><text>思考中…</text></view>
      </scroll-view>

      <view class="chips">
        <text v-for="c in quickChips" :key="c" class="chip" @click="sendQuick(c)">{{ c }}</text>
      </view>

      <view class="input-row">
        <input v-model="input" class="input" placeholder="问我行程、拼车规则…" confirm-type="send" @confirm="send" />
        <button size="mini" class="send-btn" :disabled="loading" @click="send">发送</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { sendChat } from '@/api/assistant'

const props = withDefaults(defineProps<{
  role?: 'PASSENGER' | 'DRIVER'
  pageContext?: Record<string, unknown>
  placement?: 'top' | 'bottom'
}>(), {
  role: 'DRIVER',
  placement: 'bottom',
})

const open = ref(false)
const input = ref('')
const loading = ref(false)
const sessionId = ref('')
const scrollInto = ref('')
const messages = ref<{ role: 'user' | 'assistant'; text: string }[]>([
  { role: 'assistant', text: '你好，我是拼车 AI 助手。可帮你查规则、推荐智能拼车包（最多3单同向）。' },
])

const visible = computed(() => !!uni.getStorageSync('token'))

const quickChips = computed(() =>
  props.role === 'DRIVER'
    ? ['智能拼车推荐', '怎么上线接单', '帮助']
    : ['拼车怎么用', '查我的行程', '帮助'],
)

watch(messages, () => {
  scrollInto.value = 'msg-' + (messages.value.length - 1)
}, { deep: true })

onShow(() => {
  if (!uni.getStorageSync('token')) open.value = false
})

function openPanel() {
  open.value = true
}

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
    const ctx = { ...(props.pageContext || {}), sessionId: sessionId.value, role: props.role }
    const res = await sendChat(text, ctx)
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
.ai-root {
  position: fixed;
  right: 24rpx;
  z-index: 9999;
}
.ai-root.bottom {
  bottom: 180rpx;
}
.ai-root.top {
  top: 120rpx;
}
.fab {
  width: 96rpx;
  height: 96rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #389e0d, #52c41a);
  box-shadow: 0 8rpx 24rpx rgba(56, 158, 13, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
}
.fab-icon {
  color: #fff;
  font-size: 28rpx;
  font-weight: 700;
}
.panel {
  width: 620rpx;
  max-height: 70vh;
  background: #fff;
  border-radius: 24rpx;
  box-shadow: 0 12rpx 48rpx rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.panel-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20rpx 24rpx;
  background: linear-gradient(90deg, #389e0d, #52c41a);
  color: #fff;
}
.panel-title {
  font-size: 28rpx;
  font-weight: 600;
}
.panel-close {
  font-size: 40rpx;
  line-height: 1;
  padding: 0 8rpx;
}
.msg-scroll {
  flex: 1;
  max-height: 480rpx;
  padding: 16rpx 20rpx;
}
.msg {
  margin-bottom: 16rpx;
  padding: 16rpx 20rpx;
  border-radius: 16rpx;
  font-size: 26rpx;
  line-height: 1.5;
  max-width: 90%;
}
.msg.user {
  background: #f6ffed;
  margin-left: auto;
}
.msg.assistant {
  background: #f5f5f5;
}
.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  padding: 0 16rpx 12rpx;
}
.chip {
  font-size: 22rpx;
  padding: 8rpx 16rpx;
  background: #f6ffed;
  color: #389e0d;
  border-radius: 999rpx;
}
.input-row {
  display: flex;
  gap: 12rpx;
  padding: 16rpx;
  border-top: 1rpx solid #f0f0f0;
}
.input {
  flex: 1;
  background: #f5f5f5;
  border-radius: 12rpx;
  padding: 12rpx 16rpx;
  font-size: 26rpx;
}
.send-btn {
  background: #52c41a !important;
  color: #fff !important;
}
</style>
