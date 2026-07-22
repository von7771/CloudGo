import { request } from '@/utils/request'
import { BASE_URL } from '@/config'

export interface LoginResult {
  token: string
  passengerId: number
  username: string
}

export interface UserProfile {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
  role: string
  creditScore: number | null
  balance: number | null
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
  passengerRating: number | null
  createdAt: string
}

export interface PoolMember {
  tripId: number
  startPoint: string
  endPoint: string
  status: string
  isSelf: boolean
}

export interface PoolStatus {
  poolId: number
  status: string
  currentSeats: number
  maxSeats: number
  endPoint: string
  tripStatus: string
  driverId?: number | null
  members?: PoolMember[]
}

export interface DriverLocation {
  driverId: number
  longitude: number
  latitude: number
  updatedAt: string
}

export function login(username: string, password: string) {
  return request<LoginResult>({
    url: '/api/passenger/auth/login',
    method: 'POST',
    query: { username, password },
    auth: false,
  })
}

export function register(username: string, password: string, nickname?: string) {
  return request<LoginResult>({
    url: '/api/passenger/auth/register',
    method: 'POST',
    query: { username, password, ...(nickname ? { nickname } : {}) },
    auth: false,
  })
}

export function getProfile() {
  return request<UserProfile>({ url: '/api/passenger/profile' })
}

export function updateProfile(nickname: string) {
  return request<UserProfile>({
    url: '/api/passenger/profile',
    method: 'PUT',
    query: { nickname },
  })
}

export function uploadAvatar(filePath: string) {
  const token = uni.getStorageSync('token') as string
  return new Promise<UserProfile>((resolve, reject) => {
    uni.uploadFile({
      url: `${BASE_URL}/api/passenger/profile/avatar`,
      filePath,
      name: 'file',
      header: token ? { Authorization: `Bearer ${token}` } : {},
      success: (res) => {
        try {
          const body = JSON.parse(res.data) as ApiResponse<UserProfile>
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

export function downloadAvatar(): Promise<string> {
  const token = uni.getStorageSync('token') as string
  return new Promise((resolve, reject) => {
    uni.downloadFile({
      url: `${BASE_URL}/api/passenger/profile/avatar`,
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

export function createTrip(startPoint: string, endPoint: string, tripMode: 'SOLO' | 'CARPOOL' = 'SOLO') {
  return request<Trip>({
    url: '/api/passenger/trips',
    method: 'POST',
    query: { startPoint, endPoint, tripMode },
  })
}

export function getPoolStatus(tripId: number) {
  return request<PoolStatus>({ url: `/api/passenger/trips/${tripId}/pool` })
}

export function listTrips() {
  return request<Trip[]>({ url: '/api/passenger/trips' })
}

export function getTrip(tripId: number) {
  return request<Trip>({ url: `/api/passenger/trips/${tripId}` })
}

export function getDriverLocation(tripId: number) {
  return request<DriverLocation>({ url: `/api/passenger/trips/${tripId}/driver-location` })
}

export function cancelTrip(tripId: number) {
  return request<void>({ url: `/api/passenger/trips/${tripId}/cancel`, method: 'POST' })
}

export function rateTrip(tripId: number, rating: number) {
  return request<Trip>({
    url: `/api/passenger/trips/${tripId}/rate`,
    method: 'POST',
    query: { rating },
  })
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
