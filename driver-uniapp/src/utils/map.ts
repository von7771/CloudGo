export interface MapPoint {
  latitude: number
  longitude: number
}

/** 将后端 routePoints 转为小程序 map polyline；无路线点时回退为起终点直线 */
export function buildDrivingPolyline(
  routePoints: MapPoint[],
  fallbackStart?: MapPoint | null,
  fallbackEnd?: MapPoint | null,
  options?: { color?: string; width?: number; dotted?: boolean },
): UniApp.MapPolyline[] {
  const points =
    routePoints.length >= 2
      ? routePoints
      : fallbackStart && fallbackEnd
        ? [fallbackStart, fallbackEnd]
        : []
  if (points.length < 2) return []
  return [{
    points,
    color: options?.color ?? '#52c41a',
    width: options?.width ?? 4,
    arrowLine: !options?.dotted,
    dottedLine: options?.dotted ?? false,
  }]
}

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
