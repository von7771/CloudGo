package com.von.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行程摘要，管理后台列表/详情使用。
 */
public record TripSummaryDto(
        Long id,
        Long passengerId,
        Long driverId,
        String startPoint,
        String endPoint,
        String status,
        BigDecimal estimatedAmount,
        BigDecimal finalAmount,
        Integer distanceMeters,
        Integer passengerRating,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
