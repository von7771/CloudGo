package com.von.admin.kafka;

import com.von.common.event.TripEventMessage;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class TripEventMetricsCollector implements TripEventHandler {

    private final AtomicLong eventsReceived = new AtomicLong();
    private final AtomicLong completedEvents = new AtomicLong();
    private final AtomicLong cancelledEvents = new AtomicLong();
    private final AtomicLong acceptedEvents = new AtomicLong();
    private final Map<String, AtomicLong> statusCounters = new ConcurrentHashMap<>();

    @Override
    public void handle(TripEventMessage message) {
        eventsReceived.incrementAndGet();
        statusCounters.computeIfAbsent(message.toStatus(), k -> new AtomicLong()).incrementAndGet();
        if ("COMPLETED".equals(message.toStatus())) {
            completedEvents.incrementAndGet();
        }
        if ("CANCELLED".equals(message.toStatus())) {
            cancelledEvents.incrementAndGet();
        }
        if ("ACCEPTED".equals(message.toStatus())) {
            acceptedEvents.incrementAndGet();
        }
    }

    public long getEventsReceived() {
        return eventsReceived.get();
    }

    public long getCompletedEvents() {
        return completedEvents.get();
    }

    public long getCancelledEvents() {
        return cancelledEvents.get();
    }

    public long getAcceptedEvents() {
        return acceptedEvents.get();
    }

    public Map<String, Long> getStatusCounters() {
        Map<String, Long> snapshot = new ConcurrentHashMap<>();
        statusCounters.forEach((status, counter) -> snapshot.put(status, counter.get()));
        return snapshot;
    }
}
