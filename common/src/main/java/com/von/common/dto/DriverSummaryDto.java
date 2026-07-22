package com.von.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record DriverSummaryDto(
        Long id,
        String username,
        String realName,
        String auditStatus,
        BigDecimal balance,
        LocalDateTime createdAt,
        String licenseImageUrl,
        String idCardImageUrl
) {
    public DriverSummaryDto(Long id, String username, String realName, String auditStatus,
                            BigDecimal balance, LocalDateTime createdAt) {
        this(id, username, realName, auditStatus, balance, createdAt, null, null);
    }
}
