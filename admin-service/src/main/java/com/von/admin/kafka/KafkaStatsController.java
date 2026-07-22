package com.von.admin.kafka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/kafka")
@ConditionalOnProperty(name = "app.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class KafkaStatsController {

    private final TripEventMetricsCollector metricsCollector;
    private final Optional<TripReceiptHandler> tripReceiptHandler;

    public KafkaStatsController(TripEventMetricsCollector metricsCollector,
                                Optional<TripReceiptHandler> tripReceiptHandler) {
        this.metricsCollector = metricsCollector;
        this.tripReceiptHandler = tripReceiptHandler;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("eventsReceived", metricsCollector.getEventsReceived());
        data.put("completedEvents", metricsCollector.getCompletedEvents());
        data.put("cancelledEvents", metricsCollector.getCancelledEvents());
        data.put("acceptedEvents", metricsCollector.getAcceptedEvents());
        data.put("statusCounters", metricsCollector.getStatusCounters());
        data.put("receiptsGenerated", tripReceiptHandler.map(h -> metricsCollector.getCompletedEvents()).orElse(0L));
        data.put("topic", "trip.events");
        data.put("consumerGroup", "admin-service");
        return data;
    }
}
