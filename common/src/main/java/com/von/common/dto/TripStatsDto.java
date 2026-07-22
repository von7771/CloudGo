package com.von.common.dto;

import java.math.BigDecimal;

/**
 * 管理后台 Dashboard 行程统计。
 */
public record TripStatsDto(
        long totalTrips,
        long todayTrips,
        BigDecimal totalGmv,
        BigDecimal todayGmv,
        long dispatchingCount,
        long completedCount,
        long cancelledCount
) {
}
