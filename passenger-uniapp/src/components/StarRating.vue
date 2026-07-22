<template>
  <view class="stars">
    <text
      v-for="n in 5"
      :key="n"
      class="star"
      :class="{ on: n <= modelValue }"
      @click="emit('update:modelValue', n)"
    >★</text>
  </view>
</template>

<script setup lang="ts">
defineProps<{ modelValue: number }>()
const emit = defineEmits<{ 'update:modelValue': [value: number] }>()
</script>

<style scoped>
.stars {
  display: flex;
  gap: 16rpx;
  justify-content: center;
  padding: 24rpx 0;
}
.star {
  font-size: 56rpx;
  color: #e8e8e8;
}
.star.on {
  color: #faad14;
}
</style>
