package com.von.common.event;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * 行程领域事件，经 Kafka topic {@code trip.events} 广播。
 */
public record TripEventMessage(
        Long tripId,
        String eventType,
        String fromStatus,
        String toStatus,
        Long passengerId,
        Long driverId,
        String operator,
        String remark,
        BigDecimal estimatedAmount,
        BigDecimal finalAmount,
        Instant occurredAt
) {
}
