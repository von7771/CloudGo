import { request } from '@/utils/request'
import { BASE_URL } from '@/config'

export interface LoginResult {
  token: string
  driverId: number
  realName: string
  username: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
  role: string
  balance: number | null
  auditStatus: string | null
  realName: string | null
  createdAt: string
}

export interface Trip {
  id: number
  passengerId: number
  tripMode?: string
  poolId?: number | null
  driverId: number | null
  startPoint: string
  endPoint: string
  startLocation: string | null
  endLocation: string | null
  status: string
  estimatedAmount: number
  finalAmount: number | null
  distanceMeters: number
  createdAt: string
}

export interface DocumentStatus {
  auditStatus: string
  licenseUploaded: boolean
  idCardUploaded: boolean
  licenseImageUrl?: string | null
  idCardImageUrl?: string | null
}

export function login(username: string, password: string) {
  return request<LoginResult>({
    url: '/api/driver/auth/login',
    method: 'POST',
    query: { username, password },
    auth: false,
  })
}

export function register(username: string, password: string, realName: string, nickname?: string) {
  return request<LoginResult>({
    url: '/api/driver/auth/register',
    method: 'POST',
    query: { username, password, realName, ...(nickname ? { nickname } : {}) },
    auth: false,
  })
}

export function getProfile() {
  return request<UserProfile>({ url: '/api/driver/profile' })
}

export function updateProfile(nickname?: string, realName?: string) {
  const query: Record<string, string> = {}
  if (nickname) query.nickname = nickname
  if (realName) query.realName = realName
  return request<UserProfile>({
    url: '/api/driver/profile',
    method: 'PUT',
    query,
  })
}

export function uploadAvatar(filePath: string) {
  const token = uni.getStorageSync('token') as string
  return new Promise<UserProfile>((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/driver/profile/avatar`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 401 || res.statusCode === 403) {
          reject(new Error('未登录'))
          return
        }
        if ((res.statusCode || 0) >= 400) {
          let msg = `上传失败 (${res.statusCode})`
          try {
            const body = JSON.parse(res.data) as ApiResponse<unknown>
            if (body.message) msg = body.message
          } catch {
            if (res.statusCode === 404) {
              msg = '头像接口未部署，请更新 Gateway 并重启 driver-service'
            }
          }
          reject(new Error(msg))
          return
        }
        try {
          const body = JSON.parse(res.data) as ApiResponse<UserProfile>
          if (!body.success) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch (e) {
          reject(e instanceof Error ? e : new Error('上传失败'))
        }
      },
      fail: () => reject(new Error('上传失败')),
    })
  })
}

export function downloadAvatar(): Promise<string> {
  const token = uni.getStorageSync('token') as string
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${BASE_URL}/api/driver/profile/avatar`,
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.tempFilePath)
        } else {
          reject(new Error('加载头像失败'))
        }
      },
      fail: () => reject(new Error('加载头像失败')),
    })
  })
}

export function goOnline() {
  return request<void>({ url: '/api/driver/online', method: 'POST' })
}

export function goOffline() {
  return request<void>({ url: '/api/driver/offline', method: 'POST' })
}

export function reportLocation(location: string) {
  return request<void>({
    url: '/api/driver/location',
    method: 'POST',
    query: { location },
  })
}

export function listPendingTrips() {
  return request<Trip[]>({ url: '/api/driver/trips/pending' })
}

export function listActiveTrips() {
  return request<Trip[]>({ url: '/api/driver/trips/active' })
}

export interface SmartBundle {
  bundleId: string
  similarityScore: number
  summary: string
  totalEstimatedFare: number
  trips: { tripId: number; startPoint: string; endPoint: string; estimatedAmount: number; tripMode: string }[]
}

export function listSmartBundles(limit = 5) {
  return request<SmartBundle[]>({ url: '/api/driver/trips/smart-bundles', query: { limit } })
}

export function acceptSmartBundle(tripIds: number[]) {
  const qs = tripIds.map((id) => `tripIds=${id}`).join('&')
  return request<Trip[]>({ url: `/api/driver/trips/smart-bundles/accept?${qs}`, method: 'POST' })
}

export function acceptTrip(tripId: number) {
  return request<Trip[]>({ url: `/api/driver/trips/${tripId}/accept`, method: 'POST' })
}

export function arriveTrip(tripId: number) {
  return request<Trip>({ url: `/api/driver/trips/${tripId}/arrive`, method: 'POST' })
}

export function startTrip(tripId: number) {
  return request<Trip>({ url: `/api/driver/trips/${tripId}/start`, method: 'POST' })
}

export function completeTrip(tripId: number) {
  return request<Trip>({ url: `/api/driver/trips/${tripId}/complete`, method: 'POST' })
}

export function getDocumentStatus() {
  return request<DocumentStatus>({ url: '/api/driver/documents/status' })
}

export function uploadDocument(docType: 'license' | 'id_card', filePath: string) {
  const token = uni.getStorageSync('token') as string
  return new Promise<{ docType: string; previewUrl: string }>((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/driver/documents?docType=${docType}`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResponse<{ docType: string; previewUrl: string }>
          if (!body.success) {
            reject(new Error(body.message || '上传失败'))
            return
          }
          resolve(body.data)
        } catch (e) {
          reject(e)
        }
      },
      fail: () => reject(new Error('上传失败')),
    })
  })
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
