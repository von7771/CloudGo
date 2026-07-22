package com.von.userservice.sentinel;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.von.userservice.exception.SentinelBlockedException;

import java.math.BigDecimal;

/**
 * user-service 的 Sentinel 限流降级处理类。
 * <p>
 * 方法签名必须与原 Controller 方法一致，并在末尾多一个 {@link BlockException} 参数。
 * 被 @SentinelResource 的 blockHandler / blockHandlerClass 引用。
 * </p>
 */
public final class UserSentinelBlockHandler {

    private UserSentinelBlockHandler() {
        // 工具类，禁止实例化
    }

    /**
     * getUserCredit 被限流时的降级逻辑。
     * 返回 -1，order-service 会当作信用分不足（&lt; 60）处理。
     */
    public static Integer creditBlockHandler(Long userId, BlockException ex) {
        return -1;
    }

    /**
     * deductBalance 被限流时的降级逻辑。
     * 抛出 SentinelBlockedException，由 GlobalExceptionHandler 返回 HTTP 429 + SENTINEL_BLOCKED。
     */
    public static Boolean deductBlockHandler(Long userId, BigDecimal amount, BlockException ex) {
        throw new SentinelBlockedException("扣款接口触发 Sentinel 限流，请稍后重试（userId=" + userId + "）");
    }
}
