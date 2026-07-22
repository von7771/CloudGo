export interface StatusMeta {
  label: string
  color: string
  bg: string
}

export const TRIP_STATUS: Record<string, StatusMeta> = {
  CREATED: { label: '已创建', color: '#8c8c8c', bg: '#f5f5f5' },
  DISPATCHING: { label: '派单中', color: '#1677ff', bg: '#e6f4ff' },
  ACCEPTED: { label: '已接单', color: '#722ed1', bg: '#f9f0ff' },
  ARRIVED: { label: '已到达', color: '#13c2c2', bg: '#e6fffb' },
  IN_PROGRESS: { label: '行程中', color: '#fa8c16', bg: '#fff7e6' },
  COMPLETED: { label: '已完成', color: '#52c41a', bg: '#f6ffed' },
  CANCELLED: { label: '已取消', color: '#ff4d4f', bg: '#fff1f0' },
}

export const TRIP_STEPS = [
  { key: 'DISPATCHING', label: '待接' },
  { key: 'ACCEPTED', label: '接单' },
  { key: 'ARRIVED', label: '到达' },
  { key: 'IN_PROGRESS', label: '行程' },
  { key: 'COMPLETED', label: '完单' },
]

const DRIVER_STEPS = ['ACCEPTED', 'ARRIVED', 'IN_PROGRESS', 'COMPLETED']

export function statusMeta(status: string): StatusMeta {
  return TRIP_STATUS[status] || { label: status, color: '#595959', bg: '#fafafa' }
}

export function driverStepIndex(status: string): number {
  return DRIVER_STEPS.indexOf(status)
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
