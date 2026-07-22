package com.von.common.dto;

import java.math.BigDecimal;

/**
 * 管理后台 Dashboard 汇总数据。
 */
public record DashboardDto(
        TripStatsDto tripStats,
        long onlineDriverCount,
        long pendingAuditDriverCount,
        long totalPassengerCount,
        PricingRuleDto pricing,
        DashboardChartsDto charts
) {
}
