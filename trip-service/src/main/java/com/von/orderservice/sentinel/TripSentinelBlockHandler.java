package com.von.orderservice.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.von.orderservice.exception.UserServiceBlockedException;

public final class TripSentinelBlockHandler {

    private TripSentinelBlockHandler() {
    }

    public static void createTripBlockHandler(Long passengerId, String startPoint, String endPoint, String tripMode, BlockException ex) {
        throw new UserServiceBlockedException("发单被 Sentinel 限流，请稍后重试");
    }

    public static void acceptTripBlockHandler(Long driverId, Long tripId, BlockException ex) {
        throw new UserServiceBlockedException("接单被 Sentinel 限流，请稍后重试");
    }
}
