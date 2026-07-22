package com.von.common.dto;

import java.math.BigDecimal;

public record SmartCarpoolTripItemDto(
        Long tripId,
        String startPoint,
        String endPoint,
        BigDecimal estimatedAmount,
        String tripMode
) {
}
