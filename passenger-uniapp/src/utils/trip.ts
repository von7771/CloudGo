export interface StatusMeta {
  label: string
  color: string
  bg: string
}

export const TRIP_STATUS: Record<string, StatusMeta> = {
  CREATED: { label: '已创建', color: '#8c8c8c', bg: '#f5f5f5' },
  DISPATCHING: { label: '派单中', color: '#1677ff', bg: '#e6f4ff' },
  POOL_WAITING: { label: '等待拼友', color: '#722ed1', bg: '#f9f0ff' },
  ACCEPTED: { label: '已接单', color: '#722ed1', bg: '#f9f0ff' },
  ARRIVED: { label: '已到达', color: '#13c2c2', bg: '#e6fffb' },
  IN_PROGRESS: { label: '行程中', color: '#fa8c16', bg: '#fff7e6' },
  COMPLETED: { label: '已完成', color: '#52c41a', bg: '#f6ffed' },
  CANCELLED: { label: '已取消', color: '#ff4d4f', bg: '#fff1f0' },
}

export const TRIP_STEPS = [
  { key: 'CREATED', label: '发单' },
  { key: 'DISPATCHING', label: '派单' },
  { key: 'ACCEPTED', label: '接单' },
  { key: 'ARRIVED', label: '到达' },
  { key: 'IN_PROGRESS', label: '行程' },
  { key: 'COMPLETED', label: '完成' },
]

const STATUS_ORDER = TRIP_STEPS.map((s) => s.key)

export function statusMeta(status: string): StatusMeta {
  return TRIP_STATUS[status] || { label: status, color: '#595959', bg: '#fafafa' }
}

export function stepIndex(status: string): number {
  if (status === 'CANCELLED') return -1
  // 拼车等待拼友时仍显示在「派单」阶段，避免进度条卡住
  if (status === 'POOL_WAITING') return STATUS_ORDER.indexOf('DISPATCHING')
  const idx = STATUS_ORDER.indexOf(status)
  return idx >= 0 ? idx : 0
}

export function isActiveTrip(status: string): boolean {
  return ['CREATED', 'DISPATCHING', 'POOL_WAITING', 'ACCEPTED', 'ARRIVED', 'IN_PROGRESS'].includes(status)
}

export function formatDuration(seconds: number): string {
  const m = Math.ceil(seconds / 60)
  return m < 60 ? `${m} 分钟` : `${Math.floor(m / 60)} 小时 ${m % 60} 分`
}

export function formatDistance(meters: number): string {
  return meters >= 1000 ? `${(meters / 1000).toFixed(1)} km` : `${meters} m`
}

export function parseLngLat(text: string | null | undefined) {
  if (!text || !text.includes(',')) return null
  const [lng, lat] = text.split(',').map(Number)
  if (Number.isNaN(lng) || Number.isNaN(lat)) return null
  return { latitude: lat, longitude: lng }
}

export function weatherEmoji(main: string | null | undefined): string {
  if (!main) return '🌤'
  const m = main.toLowerCase()
  if (m.includes('rain') || m.includes('drizzle')) return '🌧'
  if (m.includes('snow')) return '❄️'
  if (m.includes('cloud')) return '☁️'
  if (m.includes('clear')) return '☀️'
  if (m.includes('thunder')) return '⛈'
  return '🌤'
}
