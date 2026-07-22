<template>
  <text class="tag" :style="{ color: meta.color, backgroundColor: meta.bg }">{{ meta.label }}</text>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { statusMeta } from '@/utils/trip'

const props = defineProps<{ status: string }>()
const meta = computed(() => statusMeta(props.status))
</script>

<style scoped>
.tag {
  font-size: 22rpx;
  padding: 6rpx 16rpx;
  border-radius: 999rpx;
  font-weight: 500;
}
</style>
