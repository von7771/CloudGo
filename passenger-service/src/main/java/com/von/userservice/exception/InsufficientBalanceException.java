package com.von.userservice.exception;

import java.math.BigDecimal;

public class InsufficientBalanceException extends RuntimeException {

    public static final String ERROR_CODE = "INSUFFICIENT_BALANCE";

    private final Long passengerId;
    private final BigDecimal balance;
    private final BigDecimal requiredAmount;

    public InsufficientBalanceException(Long passengerId, BigDecimal balance, BigDecimal requiredAmount) {
        super(String.format("余额不足，当前余额 %s 元，需支付 %s 元", balance, requiredAmount));
        this.passengerId = passengerId;
        this.balance = balance;
        this.requiredAmount = requiredAmount;
    }

    public Long getPassengerId() {
        return passengerId;
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
