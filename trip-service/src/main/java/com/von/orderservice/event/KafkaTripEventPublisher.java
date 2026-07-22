package com.von.orderservice.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.von.common.event.TripEventMessage;
import com.von.orderservice.entity.Trip;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaTripEventPublisher implements TripEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaTripEventPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final String topic;

    public KafkaTripEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                   org.springframework.core.env.Environment environment) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = environment.getProperty("app.kafka.topic", "trip.events");
    }

    @Override
    public void publish(Trip trip, String fromStatus, String toStatus, String operator, String remark) {
        TripEventMessage message = new TripEventMessage(
                trip.getId(),
                toStatus,
                fromStatus,
                toStatus,
                trip.getPassengerId(),
                trip.getDriverId(),
                operator,
                remark,
                trip.getEstimatedAmount(),
                trip.getFinalAmount(),
                Instant.now()
        );
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, String.valueOf(trip.getId()), json)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.warn("Kafka trip event send failed tripId={} toStatus={}", trip.getId(), toStatus, ex);
                        } else {
                            log.debug("Kafka trip event sent tripId={} toStatus={}", trip.getId(), toStatus);
                        }
                    });
        } catch (JsonProcessingException e) {
            log.warn("Kafka trip event serialize failed tripId={}", trip.getId(), e);
        }
    }
}
