package com.von.common.dto;

import java.time.LocalDateTime;

/**
 * 司机实时位置（Redis 缓存，供地图追踪展示）。
 */
public record DriverLocationDto(
        Long driverId,
        Double longitude,
        Double latitude,
        LocalDateTime updatedAt
) {
}
