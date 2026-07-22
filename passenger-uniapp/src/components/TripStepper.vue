<template>
  <view class="stepper">
    <view v-for="(step, i) in TRIP_STEPS" :key="step.key" class="step-wrap">
      <view class="step-row">
        <view :class="['dot', dotClass(i)]">
          <text v-if="done(i)" class="check">✓</text>
          <text v-else class="num">{{ i + 1 }}</text>
        </view>
        <view v-if="i < TRIP_STEPS.length - 1" :class="['line', { done: done(i) }]" />
      </view>
      <text :class="['label', { active: current >= i, cancelled }]">{{ step.label }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { TRIP_STEPS, stepIndex } from '@/utils/trip'

const props = defineProps<{ status: string }>()
const current = computed(() => stepIndex(props.status))
const cancelled = computed(() => props.status === 'CANCELLED')

function done(i: number) {
  return !cancelled.value && current.value > i
}

function dotClass(i: number) {
  if (cancelled.value) return 'cancelled'
  if (current.value > i) return 'done'
  if (current.value === i) return 'active'
  return 'pending'
}
</script>

<style scoped>
.stepper {
  display: flex;
  justify-content: space-between;
  padding: 16rpx 0;
}
.step-wrap {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
}
.step-row {
  display: flex;
  align-items: center;
  width: 100%;
  justify-content: center;
  position: relative;
}
.dot {
  width: 44rpx;
  height: 44rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20rpx;
  z-index: 1;
}
.dot.pending {
  background: #f0f0f0;
  color: #bbb;
}
.dot.active {
  background: #1677ff;
  color: #fff;
  box-shadow: 0 0 0 6rpx rgba(22, 119, 255, 0.2);
}
.dot.done {
  background: #52c41a;
  color: #fff;
}
.dot.cancelled {
  background: #fff1f0;
  color: #ff4d4f;
}
.line {
  position: absolute;
  left: 55%;
  right: -45%;
  top: 50%;
  height: 4rpx;
  background: #f0f0f0;
  transform: translateY(-50%);
}
.line.done {
  background: #52c41a;
}
.label {
  margin-top: 12rpx;
  font-size: 20rpx;
  color: #bbb;
}
.label.active {
  color: #333;
  font-weight: 600;
}
.label.cancelled {
  color: #ff4d4f;
}
.check,
.num {
  font-size: 22rpx;
}
</style>
