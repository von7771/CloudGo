package com.von.orderservice.event;

import com.von.orderservice.entity.Trip;

public interface TripEventPublisher {

    void publish(Trip trip, String fromStatus, String toStatus, String operator, String remark);
}
