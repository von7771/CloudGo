package com.von.common.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 智能拼车推荐包：同一路线方向的多笔待接订单（最多 3 单）。
 */
public record SmartCarpoolBundleDto(
        String bundleId,
        double similarityScore,
        String summary,
        BigDecimal totalEstimatedFare,
        List<SmartCarpoolTripItemDto> trips
) {
}
