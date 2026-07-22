export interface MapPoint {
  latitude: number
  longitude: number
}

/** 折线点过多会导致微信 map 组件卡顿或不渲染，抽样保留关键点 */
export function simplifyRoutePoints(points: MapPoint[], maxPoints = 80): MapPoint[] {
  if (points.length <= maxPoints) return points
  const step = Math.ceil(points.length / maxPoints)
  const out: MapPoint[] = []
  for (let i = 0; i < points.length; i += step) {
    out.push(points[i])
  }
  const last = points[points.length - 1]
  if (out[out.length - 1] !== last) out.push(last)
  return out
}

/** 将后端 routePoints 转为小程序 map polyline；无路线点时回退为起终点直线 */
export function buildDrivingPolyline(
  routePoints: MapPoint[],
  fallbackStart?: MapPoint | null,
  fallbackEnd?: MapPoint | null,
  options?: { color?: string; width?: number; dotted?: boolean },
): UniApp.MapPolyline[] {
  const raw =
    routePoints.length >= 2
      ? routePoints
      : fallbackStart && fallbackEnd
        ? [fallbackStart, fallbackEnd]
        : []
  const points = simplifyRoutePoints(raw)
  if (points.length < 2) return []
  return [{
    points,
    color: options?.color ?? '#1677ff',
    width: options?.width ?? 4,
    arrowLine: !options?.dotted,
    dottedLine: options?.dotted ?? false,
  }]
}

/** 根据折线点计算地图中心 */
export function centerOfPoints(points: MapPoint[]): MapPoint | null {
  if (points.length === 0) return null
  let lat = 0
  let lng = 0
  for (const p of points) {
    lat += p.latitude
    lng += p.longitude
  }
  return { latitude: lat / points.length, longitude: lng / points.length }
}

export function parseRoutePoints(raw: unknown): MapPoint[] {
  if (!Array.isArray(raw)) return []
  return raw
    .map((item) => {
      const p = item as Record<string, unknown>
      const latitude = Number(p.latitude)
      const longitude = Number(p.longitude)
      if (Number.isNaN(latitude) || Number.isNaN(longitude)) return null
      return { latitude, longitude }
    })
    .filter((p): p is MapPoint => p != null)
}
