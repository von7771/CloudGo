import { BASE_URL } from '@/config'
import { parseRoutePoints, type MapPoint } from '@/utils/map'

export interface RoutePreview {
  originAddress: string
  destinationAddress: string
  originLocation: string
  destinationLocation: string
  distanceMeters: number
  durationSeconds: number
  routePoints: MapPoint[]
}

function mapRoute(raw: Record<string, unknown>): RoutePreview {
  return {
    originAddress: String(raw.originAddress || ''),
    destinationAddress: String(raw.destinationAddress || ''),
    originLocation: String(raw.originLocation || ''),
    destinationLocation: String(raw.destinationLocation || ''),
    distanceMeters: Number(raw.distanceMeters || 0),
    durationSeconds: Number(raw.durationSeconds || 0),
    routePoints: parseRoutePoints(raw.routePoints),
  }
}

export function getRoute(origin: string, destination: string): Promise<RoutePreview> {
  const qs = `origin=${encodeURIComponent(origin)}&destination=${encodeURIComponent(destination)}`
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/map/route?${qs}`,
      success: (res) => {
        const status = res.statusCode || 0
        const body = res.data as Record<string, unknown>
        if (status >= 400 || body?.success === false || body?.success === 'false') {
          reject(new Error(String(body?.message || '路线规划失败')))
          return
        }
        resolve(mapRoute(body))
      },
      fail: () => reject(new Error('网络错误')),
    })
  })
}
