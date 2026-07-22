package com.von.admin.kafka;

import com.von.common.event.TripEventMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TripNotificationSimulator implements TripEventHandler {

    private static final Logger log = LoggerFactory.getLogger(TripNotificationSimulator.class);

    @Override
    public void handle(TripEventMessage message) {
        switch (message.toStatus()) {
            case "DISPATCHING" -> log.info("[Notify] 新单 {} 已进入派单池，推送给在线司机", message.tripId());
            case "ACCEPTED" -> log.info("[Notify] 行程 {} 已被司机 {} 接单，通知乘客 passengerId={}",
                    message.tripId(), message.driverId(), message.passengerId());
            case "COMPLETED" -> log.info("[Notify] 行程 {} 已完成，扣款 {} 元，通知乘客与司机",
                    message.tripId(), message.finalAmount() != null ? message.finalAmount() : message.estimatedAmount());
            case "CANCELLED" -> log.info("[Notify] 行程 {} 已取消，通知相关方", message.tripId());
            default -> {
            }
        }
    }
}
