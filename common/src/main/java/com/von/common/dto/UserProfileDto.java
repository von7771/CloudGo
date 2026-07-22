package com.von.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserProfileDto(
        Long id,
        String username,
        String nickname,
        String avatarUrl,
        String role,
        Integer creditScore,
        BigDecimal balance,
        String auditStatus,
        String realName,
        LocalDateTime createdAt
) {
}
