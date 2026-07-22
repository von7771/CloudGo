import { BASE_URL } from '@/config'

import { parseRoutePoints, type MapPoint } from '@/utils/map'

export interface RoutePreview {
  originAddress: string
  destinationAddress: string
  originLocation: string
  destinationLocation: string
  distanceMeters: number
  durationSeconds: number
  baseFare: number
  estimatedFare: number
  weatherMain: string | null
  weatherDescription: string | null
  temperatureCelsius: number | null
  weatherMultiplier: number
  weatherSurcharge: number
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
    baseFare: Number(raw.baseFare || 0),
    estimatedFare: Number(raw.estimatedFare || 0),
    weatherMain: (raw.weatherMain as string) || null,
    weatherDescription: (raw.weatherDescription as string) || null,
    temperatureCelsius: raw.temperatureCelsius != null ? Number(raw.temperatureCelsius) : null,
    weatherMultiplier: Number(raw.weatherMultiplier || 1),
    weatherSurcharge: Number(raw.weatherSurcharge || 0),
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
        if (status >= 400) {
          const msg = String(body?.message || '路线规划失败')
          uni.showToast({ title: msg.slice(0, 40), icon: 'none', duration: 3000 })
          reject(new Error(msg))
          return
        }
        if (body?.success === false || body?.success === 'false') {
          const msg = String(body?.message || '路线规划失败')
          uni.showToast({ title: msg.slice(0, 40), icon: 'none', duration: 3000 })
          reject(new Error(msg))
          return
        }
        const payload =
          body?.data && typeof body.data === 'object' && !Array.isArray(body.data)
            ? (body.data as Record<string, unknown>)
            : body
        resolve(mapRoute(payload))
      },
      fail: () => {
        uni.showToast({ title: '无法连接 Gateway，请检查 port-forward', icon: 'none', duration: 3000 })
        reject(new Error('网络错误'))
      },
    })
  })
}
