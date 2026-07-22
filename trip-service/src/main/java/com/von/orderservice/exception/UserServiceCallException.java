package com.von.orderservice.exception;

/**
 * 调用 user-service 失败，但不属于「余额不足」或「Sentinel 限流」的其他错误。
 * <p>
 * 例如：user-service 宕机、网络超时、HTTP 500 等。
 * GlobalExceptionHandler 会返回 HTTP 502 Bad Gateway。
 * </p>
 */
public class UserServiceCallException extends RuntimeException {

    /** 表示远程用户服务调用异常，但不是上述两类已知业务错误 */
    public static final String ERROR_CODE = "USER_SERVICE_ERROR";

    /**
     * @param message 包含操作名和底层原因的描述
     */
    public UserServiceCallException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
