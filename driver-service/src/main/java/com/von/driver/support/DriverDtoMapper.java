package com.von.driver.support;

import com.von.common.dto.DriverSummaryDto;
import com.von.driver.entity.Driver;

public final class DriverDtoMapper {

    private DriverDtoMapper() {
    }

    public static DriverSummaryDto toSummary(Driver driver) {
        return new DriverSummaryDto(
                driver.getId(),
                driver.getUsername(),
                driver.getRealName(),
                driver.getAuditStatus(),
                driver.getBalance(),
                driver.getCreatedAt()
        );
    }
}
