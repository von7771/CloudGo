package com.von.orderservice.support;

import com.von.common.dto.TripSummaryDto;
import com.von.orderservice.entity.Trip;

public final class TripDtoMapper {

    private TripDtoMapper() {
    }

    public static TripSummaryDto toSummary(Trip trip) {
        return new TripSummaryDto(
                trip.getId(),
                trip.getPassengerId(),
                trip.getDriverId(),
                trip.getStartPoint(),
                trip.getEndPoint(),
                trip.getStatus(),
                trip.getEstimatedAmount(),
                trip.getFinalAmount(),
                trip.getDistanceMeters(),
                trip.getPassengerRating(),
                trip.getCreatedAt(),
                trip.getUpdatedAt()
        );
    }
}
