package com.von.common.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 管理后台可视化图表数据。
 */
public record DashboardChartsDto(
        List<String> last7Days,
        List<Long> tripTrend,
        List<BigDecimal> gmvTrend,
        long soloCount,
        long carpoolCount,
        long acceptedCount,
        long inProgressCount,
        List<String> hourlyLabels,
        List<Long> hourlyTrips
) {
}
