import request from '@/utils/request'
import type {
  ApiResponse,
  Dashboard,
  DriverLocation,
  DriverSummary,
  LoginResult,
  PageResult,
  PassengerSummary,
  PricingRule,
  TripSummary,
} from '@/types/api'

export function login(username: string, password: string) {
  return request.post<ApiResponse<LoginResult>>('/api/admin/auth/login', null, {
    params: { username, password },
  })
}

export function getDashboard() {
  return request.get<ApiResponse<Dashboard>>('/api/admin/dashboard')
}

export function listTrips(params: { status?: string; page?: number; size?: number }) {
  return request.get<ApiResponse<PageResult<TripSummary>>>('/api/admin/trips', { params })
}

export function getTrip(tripId: number) {
  return request.get<ApiResponse<TripSummary>>(`/api/admin/trips/${tripId}`)
}

export function getTripReceiptUrl(tripId: number) {
  return request.get<ApiResponse<{ receiptUrl?: string }>>(`/api/admin/trips/${tripId}/receipt-url`)
}

export function listDrivers(params: { auditStatus?: string; page?: number; size?: number }) {
  return request.get<ApiResponse<PageResult<DriverSummary>>>('/api/admin/drivers', { params })
}

export function auditDriver(driverId: number, auditStatus: string) {
  return request.put<ApiResponse<DriverSummary>>(`/api/admin/drivers/${driverId}/audit`, null, {
    params: { auditStatus },
  })
}

export function listPassengers(params: { page?: number; size?: number }) {
  return request.get<ApiResponse<PageResult<PassengerSummary>>>('/api/admin/passengers', { params })
}

export function banPassenger(passengerId: number, banned: boolean) {
  return request.put<ApiResponse<Record<string, unknown>>>(
    `/api/admin/passengers/${passengerId}/ban`,
    null,
    { params: { banned } },
  )
}

export function getPricing() {
  return request.get<ApiResponse<PricingRule>>('/api/admin/pricing')
}

export function updatePricing(data: PricingRule) {
  return request.put<ApiResponse<PricingRule>>('/api/admin/pricing', null, { params: data })
}

/** 返回在线司机位置列表（直接 JSON 数组，非 ApiResponse 包装） */
export async function listDriverLocations(): Promise<DriverLocation[]> {
  const res = await request.get<DriverLocation[]>('/api/map/drivers/locations')
  return Array.isArray(res.data) ? res.data : []
}
