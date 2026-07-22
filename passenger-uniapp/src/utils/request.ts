import { BASE_URL } from '@/config'

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

type Method = 'GET' | 'POST' | 'PUT'

export function request<T>(options: {
  url: string
  method?: Method
  query?: Record<string, string | number | boolean>
  auth?: boolean
}): Promise<T> {
  const token = uni.getStorageSync('token') as string
  let url = BASE_URL + options.url
  if (options.query) {
    const qs = Object.entries(options.query)
      .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
      .join('&')
    url += (url.includes('?') ? '&' : '?') + qs
  }

  return new Promise((resolve, reject) => {
    let settled = false
    const timer = setTimeout(() => {
      if (settled) return
      settled = true
      uni.showToast({ title: '请求超时，请稍后重试', icon: 'none' })
      reject(new Error('请求超时'))
    }, 15000)

    uni.request({
      url,
      method: options.method || 'GET',
      header: {
        ...(options.auth !== false && token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success: (res) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        const status = res.statusCode || 0
        if (status === 401 || status === 403) {
          uni.removeStorageSync('token')
          uni.reLaunch({ url: '/pages/login/login' })
          reject(new Error('未登录'))
          return
        }
        const body = res.data as ApiResponse<T>
        if (body && body.success === false) {
          uni.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message))
          return
        }
        resolve(body?.data ?? (res.data as T))
      },
      fail: (err) => {
        if (settled) return
        settled = true
        clearTimeout(timer)
        uni.showToast({ title: '网络错误', icon: 'none' })
        reject(err)
      },
    })
  })
}
