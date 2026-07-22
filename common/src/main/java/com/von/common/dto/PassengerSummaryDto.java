package com.von.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PassengerSummaryDto(
        Long id,
        String username,
        Integer creditScore,
        BigDecimal balance,
        LocalDateTime createdAt
) {
}
