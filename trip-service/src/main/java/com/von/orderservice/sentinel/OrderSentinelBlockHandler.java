package com.von.orderservice.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.von.orderservice.exception.UserServiceBlockedException;

/**
 * order-service 的 Sentinel 限流降级处理类。
 * <p>
 * 对应 {@link com.von.orderservice.controller.OrderController#createOrder} 上的 @SentinelResource。
 * 方法参数必须与原接口一致，末尾追加 {@link BlockException}。
 * </p>
 */
public final class OrderSentinelBlockHandler {

    private OrderSentinelBlockHandler() {
        // 工具类，禁止实例化
    }

    /**
     * createOrder 被 Sentinel QPS 限流时调用。
     * <p>
     * 抛出 {@link UserServiceBlockedException}，由 GlobalExceptionHandler 返回
     * HTTP 429 + errorCode=SENTINEL_BLOCKED，与余额不足（400）区分。
     * </p>
     *
     * @param userId     原接口参数：用户 ID
     * @param startPoint 原接口参数：起点
     * @param endPoint   原接口参数：终点
     * @param ex         Sentinel 提供的阻塞异常（含限流规则信息）
     */
    public static String createOrderBlockHandler(Long userId, String startPoint, String endPoint, BlockException ex) {
        throw new UserServiceBlockedException(
                "创建订单被 Sentinel 限流，请稍后重试（userId=" + userId + "）"
        );
    }
}
