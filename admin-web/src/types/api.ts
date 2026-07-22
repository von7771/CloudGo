export interface ApiResponse<T> {
  success: boolean
  errorCode: string
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

export interface LoginResult {
  token: string
  adminId: number
  role: string
}

export interface TripStats {
  totalTrips: number
  todayTrips: number
  totalGmv: number
  todayGmv: number
  dispatchingCount: number
  completedCount: number
  cancelledCount: number
}

export interface PricingRule {
  baseFare: number
  perKmRate: number
  minFare: number
}

export interface DashboardCharts {
  last7Days: string[]
  tripTrend: number[]
  gmvTrend: number[]
  soloCount: number
  carpoolCount: number
  acceptedCount: number
  inProgressCount: number
  hourlyLabels: string[]
  hourlyTrips: number[]
}

export interface Dashboard {
  tripStats: TripStats
  onlineDriverCount: number
  pendingAuditDriverCount: number
  totalPassengerCount: number
  pricing: PricingRule
  charts: DashboardCharts
}

export interface TripSummary {
  id: number
  passengerId: number
  driverId: number | null
  startPoint: string
  endPoint: string
  status: string
  estimatedAmount: number
  finalAmount: number | null
  distanceMeters: number
  passengerRating: number | null
  createdAt: string
  updatedAt: string
}

export interface DriverSummary {
  id: number
  username: string
  realName: string
  auditStatus: string
  balance: number
  createdAt: string
  licenseImageUrl?: string | null
  idCardImageUrl?: string | null
}

export interface PassengerSummary {
  id: number
  username: string
  creditScore: number
  balance: number
  createdAt: string
}

export interface DriverLocation {
  driverId: number
  longitude: number
  latitude: number
  updatedAt: string
}
