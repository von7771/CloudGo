<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { listDriverLocations } from '@/api/admin'
import type { DriverLocation } from '@/types/api'

const loading = ref(false)
const locations = ref<DriverLocation[]>([])
const autoRefresh = ref(true)
const mapContainer = ref<HTMLElement | null>(null)

let map: L.Map | null = null
let markerLayer: L.LayerGroup | null = null
let timer: ReturnType<typeof setInterval> | null = null

/** 国内可访问的高德瓦片（GCJ-02），不依赖 openstreetmap.org */
function createTileLayer() {
  return L.tileLayer(
    'https://webrd0{s}.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
    {
      subdomains: ['1', '2', '3', '4'],
      maxZoom: 18,
      attribution: '© 高德地图',
    },
  )
}

const DRIVER_COLORS = ['#52c41a', '#1677ff', '#fa8c16', '#eb2f96', '#722ed1', '#13c2c2']

function createDriverIcon(driverId: number) {
  const color = DRIVER_COLORS[driverId % DRIVER_COLORS.length]
  return L.divIcon({
    className: 'driver-marker-wrap',
    html: `<div class="driver-marker" style="background:${color}">司机${driverId}</div>`,
    iconSize: [56, 28],
    iconAnchor: [28, 14],
  })
}

function initMap() {
  if (!mapContainer.value || map) return

  map = L.map(mapContainer.value, {
    center: [23.04, 113.33],
    zoom: 12,
    zoomControl: true,
  })

  createTileLayer().addTo(map)
  markerLayer = L.layerGroup().addTo(map)
}

function updateMarkers(items: DriverLocation[]) {
  if (!map || !markerLayer) return

  markerLayer.clearLayers()
  const points: L.LatLngExpression[] = []

  items.forEach((loc) => {
    if (loc.latitude == null || loc.longitude == null) return
    const latlng: L.LatLngExpression = [loc.latitude, loc.longitude]
    points.push(latlng)

    const marker = L.marker(latlng, { icon: createDriverIcon(loc.driverId) })
      .bindPopup(
        `<b>司机 #${loc.driverId}</b><br>经度 ${loc.longitude.toFixed(6)}<br>纬度 ${loc.latitude.toFixed(6)}<br>更新 ${loc.updatedAt || '-'}`,
      )
    markerLayer!.addLayer(marker)
  })

  if (points.length === 1) {
    map.setView(points[0], 14)
  } else if (points.length > 1) {
    map.fitBounds(L.latLngBounds(points), { padding: [48, 48] })
  }
}

async function load() {
  loading.value = true
  try {
    locations.value = await listDriverLocations()
    updateMarkers(locations.value)
  } catch {
    locations.value = []
    updateMarkers([])
  } finally {
    loading.value = false
  }
}

function startTimer() {
  stopTimer()
  if (autoRefresh.value) {
    timer = setInterval(load, 60000)
  }
}

function stopTimer() {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

function toggleAutoRefresh(val: boolean) {
  autoRefresh.value = val
  if (val) startTimer()
  else stopTimer()
}

onMounted(async () => {
  await nextTick()
  initMap()
  load()
  startTimer()
})

onBeforeUnmount(() => {
  stopTimer()
  if (map) {
    map.remove()
    map = null
    markerLayer = null
  }
})
</script>

<template>
  <el-card shadow="never">
    <div class="toolbar">
      <el-button type="primary" :loading="loading" @click="load">立即刷新</el-button>
      <el-switch
        v-model="autoRefresh"
        active-text="每60秒自动刷新"
        @change="toggleAutoRefresh"
      />
      <span class="hint">地图内嵌展示，无需跳转外站；需司机端上线并上报位置</span>
    </div>

    <div ref="mapContainer" class="map-container" v-loading="loading" />

    <el-alert
      v-if="locations.length === 0 && !loading"
      title="地图上暂无司机标记：请先在司机端登录、上线并开始上报位置"
      type="info"
      show-icon
      :closable="false"
      style="margin-top: 12px"
    />

    <el-table :data="locations" v-loading="loading" stripe style="margin-top: 16px">
      <el-table-column prop="driverId" label="司机ID" width="90" />
      <el-table-column label="经度" width="120">
        <template #default="{ row }">{{ row.longitude?.toFixed(6) }}</template>
      </el-table-column>
      <el-table-column label="纬度" width="120">
        <template #default="{ row }">{{ row.latitude?.toFixed(6) }}</template>
      </el-table-column>
      <el-table-column prop="updatedAt" label="更新时间" min-width="180" />
    </el-table>
  </el-card>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.hint {
  color: #888;
  font-size: 13px;
}

.map-container {
  width: 100%;
  height: 480px;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  z-index: 0;
}

:global(.driver-marker-wrap) {
  background: transparent;
  border: none;
}

:global(.driver-marker) {
  color: #fff;
  font-size: 12px;
  padding: 4px 8px;
  border-radius: 14px;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.25);
  text-align: center;
  white-space: nowrap;
}
</style>
