package com.von.orderservice.event;

import com.von.orderservice.entity.Trip;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "false")
public class NoOpTripEventPublisher implements TripEventPublisher {

    @Override
    public void publish(Trip trip, String fromStatus, String toStatus, String operator, String remark) {
        // Kafka 未启用时跳过
    }
}
