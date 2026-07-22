package com.von.orderservice.exception;

/**
 * Sentinel 限流/熔断异常（order-service 侧）。
 * <p>
 * 可能来源：
 * <ul>
 *   <li>user-service 扣款/查信用分接口被 Sentinel 拦截（Feign 收到 429）</li>
 *   <li>order-service 自身 createOrder 接口 QPS 超限（OrderSentinelBlockHandler）</li>
 * </ul>
 * </p>
 */
public class UserServiceBlockedException extends RuntimeException {

    /** 固定错误码，与 user-service 的 SentinelBlockedException 保持一致 */
    public static final String ERROR_CODE = "SENTINEL_BLOCKED";

    /**
     * @param message 限流提示文案
     */
    public UserServiceBlockedException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
