import { WS_URL, DEMO_ROUTE } from '@/config'
import { goOffline, goOnline, listPendingTrips, reportLocation, type Trip } from '@/api/driver'

const ONLINE_KEY = 'driverOnline'

export interface DriverSessionHooks {
  onOnlineChange: (online: boolean) => void
  onWsChange: (connected: boolean) => void
  onLocationReported: (location: string) => void
  onPendingChange: (trips: Trip[]) => void
  onLog: (msg: string) => void
  onCountdown: (seconds: number) => void
}

let socketTask: UniApp.SocketTask | null = null
let socketOpen = false
let connecting = false
let globalSocketBound = false
let reconnectTimer: ReturnType<typeof setTimeout> | null = null
let locationTimer: ReturnType<typeof setInterval> | null = null
let countdownTimer: ReturnType<typeof setInterval> | null = null
let pendingTimer: ReturnType<typeof setInterval> | null = null
let routeIndex = 0
let locCountdown = 60
let hooksRef: DriverSessionHooks | null = null

export function isDriverOnline(): boolean {
  return uni.getStorageSync(ONLINE_KEY) === true
}

function setOnlineFlag(online: boolean) {
  uni.setStorageSync(ONLINE_KEY, online)
}

export function isWsConnected(): boolean {
  return socketOpen
}

function resolveLocation(): Promise<string> {
  return new Promise((resolve) => {
    uni.getLocation({
      type: 'gcj02',
      success: (res) => resolve(`${res.longitude},${res.latitude}`),
      fail: () => {
        const [lng, lat] = DEMO_ROUTE[routeIndex % DEMO_ROUTE.length]
        routeIndex++
        resolve(`${lng},${lat}`)
      },
    })
  })
}

async function reportCurrentLocation() {
  if (!hooksRef || !isDriverOnline()) return
  const location = await resolveLocation()
  await reportLocation(location)
  locCountdown = 60
  hooksRef.onLocationReported(location)
  hooksRef.onLog(`位置上报 ${location}`)
}

function startLocationLoop() {
  stopLocationLoop()
  locCountdown = 60
  locationTimer = setInterval(() => {
    reportCurrentLocation().catch(() => {})
  }, 60000)
  countdownTimer = setInterval(() => {
    locCountdown = locCountdown <= 1 ? 60 : locCountdown - 1
    hooksRef?.onCountdown(locCountdown)
  }, 1000)
}

function stopLocationLoop() {
  if (locationTimer) {
    clearInterval(locationTimer)
    locationTimer = null
  }
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
}

async function refreshPending() {
  if (!hooksRef || !isDriverOnline()) return
  try {
    const trips = await listPendingTrips()
    hooksRef.onPendingChange(trips)
  } catch {
    /* ignore */
  }
}

function startPendingPoll() {
  stopPendingPoll()
  pendingTimer = setInterval(() => {
    refreshPending().catch(() => {})
  }, 10000)
}

function stopPendingPoll() {
  if (pendingTimer) {
    clearInterval(pendingTimer)
    pendingTimer = null
  }
}

function onSocketOpened() {
  connecting = false
  socketOpen = true
  hooksRef?.onWsChange(true)
  hooksRef?.onLog('实时推送已连接')
}

function onSocketMessage(res: UniApp.OnSocketMessageCallbackResult) {
  hooksRef?.onLog(`新单推送: ${res.data}`)
  uni.vibrateShort({})
  refreshPending().catch(() => {})
}

function onSocketClosed() {
  const wasOpen = socketOpen
  socketOpen = false
  connecting = false
  socketTask = null
  hooksRef?.onWsChange(false)
  if (wasOpen && isDriverOnline()) {
    scheduleReconnect()
  }
}

function onSocketFailed() {
  connecting = false
  socketOpen = false
  socketTask = null
  hooksRef?.onWsChange(false)
  hooksRef?.onLog('实时推送连接失败，已切换轮询模式')
  if (isDriverOnline()) {
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (reconnectTimer) return
  reconnectTimer = setTimeout(() => {
    reconnectTimer = null
    if (isDriverOnline() && !socketOpen && !connecting) {
      connectWs().catch(() => {})
    }
  }, 5000)
}

function clearReconnect() {
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
}

function bindGlobalSocketOnce() {
  if (globalSocketBound) return
  globalSocketBound = true
  uni.onSocketOpen(onSocketOpened)
  uni.onSocketMessage(onSocketMessage)
  uni.onSocketClose(onSocketClosed)
  uni.onSocketError(onSocketFailed)
}

function disconnectWsSafe() {
  clearReconnect()
  if (!socketOpen && !connecting) {
    socketTask = null
    return
  }
  socketOpen = false
  connecting = false
  if (socketTask && typeof socketTask.close === 'function') {
    try {
      socketTask.close({ code: 1000, reason: 'close' })
    } catch {
      /* ignore */
    }
  } else {
    try {
      uni.closeSocket({ code: 1000, reason: 'close' })
    } catch {
      /* ignore */
    }
  }
  socketTask = null
}

async function connectWs() {
  if (connecting || socketOpen) return
  if (!isDriverOnline()) return

  connecting = true
  bindGlobalSocketOnce()

  try {
    const result = uni.connectSocket({
      url: WS_URL,
      fail: () => onSocketFailed(),
    })
    if (result && typeof (result as Promise<UniApp.SocketTask>).then === 'function') {
      socketTask = await (result as Promise<UniApp.SocketTask>)
    } else if (result) {
      socketTask = result as UniApp.SocketTask
    }
  } catch {
    onSocketFailed()
  }
}

export async function startDriverSession(hooks: DriverSessionHooks) {
  hooksRef = hooks
  try {
    await goOnline()
    setOnlineFlag(true)
    hooks.onOnlineChange(true)
    hooks.onLog('已上线，开始接单')

    await reportCurrentLocation()
    startLocationLoop()
    await connectWs()
    await refreshPending()
    startPendingPoll()
    uni.vibrateShort({})
  } catch (err) {
    setOnlineFlag(false)
    hooks.onOnlineChange(false)
    throw err
  }
}

export async function stopDriverSession(callOfflineApi = true) {
  stopLocationLoop()
  stopPendingPoll()
  setOnlineFlag(false)
  hooksRef?.onOnlineChange(false)
  hooksRef?.onWsChange(false)
  disconnectWsSafe()

  if (callOfflineApi) {
    try {
      await goOffline()
      hooksRef?.onLog('已下线')
    } catch {
      hooksRef?.onLog('下线请求失败')
    }
  }
}

/** Tab 切走时调用：保持上线，仅暂停 UI 回调 */
export function detachSessionHooks() {
  hooksRef = null
}

/** 回到工作台时恢复 UI 回调，并确保后台任务仍在运行 */
export async function attachSessionHooks(hooks: DriverSessionHooks) {
  hooksRef = hooks
  hooks.onOnlineChange(isDriverOnline())
  hooks.onWsChange(socketOpen)
  hooks.onCountdown(locCountdown)

  if (!isDriverOnline()) return

  if (!locationTimer) startLocationLoop()
  if (!pendingTimer) startPendingPoll()
  if (!socketOpen && !connecting) await connectWs()
  await refreshPending()
}

export function clearDriverSessionOnLogout() {
  stopLocationLoop()
  stopPendingPoll()
  setOnlineFlag(false)
  disconnectWsSafe()
  hooksRef = null
}
