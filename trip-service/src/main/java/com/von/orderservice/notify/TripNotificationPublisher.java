package com.von.orderservice.notify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.von.orderservice.entity.Trip;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class TripNotificationPublisher {

    public static final String CHANNEL = "trip:new";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public TripNotificationPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    public void publishNewTrip(Trip trip) {
        TripPushMessage message = new TripPushMessage(
                trip.getId(),
                trip.getStartPoint(),
                trip.getEndPoint(),
                trip.getEstimatedAmount(),
                trip.getStatus()
        );
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("推送消息序列化失败", e);
        }
    }
}
