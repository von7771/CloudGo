package com.von.admin.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.von.common.event.TripEventMessage;
import com.von.common.storage.MinioStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@ConditionalOnProperty(name = "app.minio.enabled", havingValue = "true", matchIfMissing = true)
public class TripReceiptHandler implements TripEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TripReceiptHandler.class);

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MinioStorageService minioStorageService;
    private final Map<Long, String> receiptObjectKeys = new ConcurrentHashMap<>();

    public TripReceiptHandler(MinioStorageService minioStorageService) {
        this.minioStorageService = minioStorageService;
    }

    @Override
    public void handle(TripEventMessage message) {
        if (!"COMPLETED".equals(message.toStatus())) {
            return;
        }
        try {
            Map<String, Object> receipt = new LinkedHashMap<>();
            receipt.put("tripId", message.tripId());
            receipt.put("passengerId", message.passengerId());
            receipt.put("driverId", message.driverId());
            receipt.put("amount", message.finalAmount() != null ? message.finalAmount() : message.estimatedAmount());
            receipt.put("completedAt", message.occurredAt());
            receipt.put("title", "拼车行程电子凭证");

            byte[] bytes = objectMapper.writeValueAsBytes(receipt);
            String objectKey = "trips/" + message.tripId() + "/receipt.json";
            minioStorageService.upload(objectKey, new ByteArrayInputStream(bytes), bytes.length, "application/json");
            receiptObjectKeys.put(message.tripId(), objectKey);
            log.info("[MinIO] trip receipt uploaded tripId={} objectKey={}", message.tripId(), objectKey);
        } catch (Exception e) {
            log.warn("[MinIO] trip receipt upload failed tripId={}", message.tripId(), e);
        }
    }

    public Optional<String> getReceiptUrl(Long tripId) {
        String objectKey = receiptObjectKeys.getOrDefault(tripId, "trips/" + tripId + "/receipt.json");
        return Optional.of("/api/admin/trips/" + tripId + "/receipt");
    }

    public byte[] downloadReceipt(Long tripId) {
        String objectKey = receiptObjectKeys.getOrDefault(tripId, "trips/" + tripId + "/receipt.json");
        return minioStorageService.download(objectKey);
    }
}
