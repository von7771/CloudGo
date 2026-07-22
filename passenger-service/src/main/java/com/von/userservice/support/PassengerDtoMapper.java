package com.von.userservice.support;

import com.von.common.dto.PassengerSummaryDto;
import com.von.userservice.entity.Passenger;

public final class PassengerDtoMapper {

    private PassengerDtoMapper() {
    }

    public static PassengerSummaryDto toSummary(Passenger passenger) {
        return new PassengerSummaryDto(
                passenger.getId(),
                passenger.getUsername(),
                passenger.getCreditScore(),
                passenger.getBalance(),
                passenger.getCreatedAt()
        );
    }
}
