package com.von.orderservice.notify;

import java.math.BigDecimal;

/**
 * 新行程推送给司机的 WebSocket 消息体。
 */
public record TripPushMessage(
        Long tripId,
        String startPoint,
        String endPoint,
        BigDecimal estimatedAmount,
        String status
) {
}
