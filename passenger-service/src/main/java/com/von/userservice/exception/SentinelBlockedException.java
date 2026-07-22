package com.von.userservice.exception;

/**
 * user-service 接口被 Sentinel 限流或熔断时抛出的业务异常。
 * <p>
 * 由 {@link com.von.userservice.sentinel.UserSentinelBlockHandler} 在 QPS 超限时抛出，
 * GlobalExceptionHandler 将其转为 HTTP 429 + errorCode={@link #ERROR_CODE}。
 * </p>
 */
public class SentinelBlockedException extends RuntimeException {

    /** 固定错误码，表示 Sentinel 限流/熔断，而非余额不足或系统 bug */
    public static final String ERROR_CODE = "SENTINEL_BLOCKED";

    /**
     * @param message 限流提示，如「扣款接口触发 Sentinel 限流，请稍后重试」
     */
    public SentinelBlockedException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
