package com.von.admin.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.von.common.event.TripEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class TripEventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(TripEventDispatcher.class);

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final List<TripEventHandler> handlers;

    public TripEventDispatcher(List<TripEventHandler> handlers) {
        this.handlers = handlers;
    }

    @KafkaListener(topics = "${app.kafka.topic:trip.events}", groupId = "admin-service")
    public void onTripEvent(String payload) {
        try {
            TripEventMessage message = objectMapper.readValue(payload, TripEventMessage.class);
            log.info("[Kafka] tripId={} {} -> {} operator={}",
                    message.tripId(), message.fromStatus(), message.toStatus(), message.operator());
            for (TripEventHandler handler : handlers) {
                try {
                    handler.handle(message);
                } catch (Exception e) {
                    log.warn("[Kafka] handler {} failed tripId={}", handler.getClass().getSimpleName(),
                            message.tripId(), e);
                }
            }
        } catch (Exception e) {
            log.warn("[Kafka] failed to parse trip event: {}", payload, e);
        }
    }
}
