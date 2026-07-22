import { request } from '@/utils/request'
import { BASE_URL } from '@/config'

export interface ChatResponse {
  reply: string
  sessionId: string
  toolUsed: string[]
  fromCache: boolean
}

export interface SmartBundle {
  bundleId: string
  similarityScore: number
  summary: string
  totalEstimatedFare: number
  trips: { tripId: number; startPoint: string; endPoint: string; estimatedAmount: number; tripMode: string }[]
}

export function sendChat(message: string, context?: Record<string, unknown>): Promise<ChatResponse> {
  const token = uni.getStorageSync('token') as string
  return new Promise((resolve, reject) => {
    uni.request({
      url: `${BASE_URL}/api/assistant/chat`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      data: { message, context },
      success: (res) => {
        const body = res.data as { success?: boolean; message?: string; data?: ChatResponse }
        if (body?.success === false || !body?.data) {
          reject(new Error(body?.message || 'AI 服务不可用'))
          return
        }
        resolve(body.data)
      },
      fail: () => reject(new Error('网络错误')),
    })
  })
}
