package com.von.userservice.controller;

import com.von.userservice.exception.InsufficientBalanceException;
import com.von.userservice.exception.SentinelBlockedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * user-service 全局异常处理器。
 * <p>
 * 把 Java 异常统一转成结构化 JSON，并通过 errorCode 区分：
 * 余额不足（400）、Sentinel 限流（429）、参数错误（400）、其他内部错误（500）。
 * Feign 调用方（order-service）依赖此 JSON 结构做二次翻译。
 * </p>
 */
@RestControllerAdvice // 拦截本服务所有 @RestController 抛出的异常
public class GlobalExceptionHandler {

    /**
     * 处理参数/业务校验类错误（如用户不存在）。
     * HTTP 400，errorCode=BAD_REQUEST。
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseError("BAD_REQUEST", e.getMessage()));
    }

    /**
     * 处理余额不足。
     * HTTP 400，errorCode=INSUFFICIENT_BALANCE，并附带 userId/balance/requiredAmount 便于前端展示。
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException e) {
        Map<String, Object> body = baseError(e.getErrorCode(), e.getMessage());
        body.put("passengerId", e.getPassengerId());
        body.put("balance", e.getBalance());                 // 当前余额
        body.put("requiredAmount", e.getRequiredAmount());   // 需要支付多少
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * 处理 Sentinel 限流/熔断。
     * HTTP 429 Too Many Requests，errorCode=SENTINEL_BLOCKED。
     */
    @ExceptionHandler(SentinelBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleSentinelBlocked(SentinelBlockedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(baseError(e.getErrorCode(), e.getMessage()));
    }

    /**
     * 兜底：未单独处理的异常一律 500。
     * 避免把堆栈直接返回，只返回 message 或异常类名。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(baseError("INTERNAL_ERROR", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
    }

    /**
     * 构造统一错误 JSON 的基础字段。
     *
     * @param errorCode 错误分类码，供调用方程序化处理
     * @param message   人类可读的错误描述
     */
    private Map<String, Object> baseError(String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>(); // LinkedHashMap 保证 JSON 字段顺序稳定
        body.put("success", "false");      // 固定表示失败
        body.put("errorCode", errorCode);  // 错误类型码
        body.put("message", message);      // 错误详情
        return body;
    }
}
