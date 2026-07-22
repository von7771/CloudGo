package com.von.orderservice.exception;

import java.math.BigDecimal;

/**
 * order-service 侧的「余额不足」业务异常。
 * <p>
 * 并非本地扣款产生，而是 Feign 调用 user-service 收到 HTTP 400 + errorCode=INSUFFICIENT_BALANCE 后，
 * 由 {@link com.von.orderservice.feign.UserFeignExceptionTranslator} 翻译而来，
 * 再由 {@link com.von.orderservice.controller.GlobalExceptionHandler} 返回给 Gateway/Postman。
 * </p>
 */
public class InsufficientBalanceException extends RuntimeException {

    /** 与 user-service 保持一致的错误码，便于前后端统一处理 */
    public static final String ERROR_CODE = "INSUFFICIENT_BALANCE";

    /** 余额不足的用户 ID（从 user-service JSON 解析，可能为 null） */
    private final Long userId;

    /** 当前余额（元） */
    private final BigDecimal balance;

    /** 需支付金额（元） */
    private final BigDecimal requiredAmount;

    /**
     * @param userId         用户 ID
     * @param balance        当前余额
     * @param requiredAmount 需支付金额
     * @param message        展示给用户的错误描述
     */
    public InsufficientBalanceException(Long userId, BigDecimal balance, BigDecimal requiredAmount, String message) {
        super(message);
        this.userId = userId;
        this.balance = balance;
        this.requiredAmount = requiredAmount;
    }

    public Long getUserId() {
        return userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getRequiredAmount() {
        return requiredAmount;
    }

    public String getErrorCode() {
        return ERROR_CODE;
    }
}
