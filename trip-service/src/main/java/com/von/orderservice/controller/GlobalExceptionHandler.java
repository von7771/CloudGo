package com.von.orderservice.controller;

import com.von.orderservice.exception.InsufficientBalanceException;
import com.von.orderservice.exception.UserServiceBlockedException;
import com.von.orderservice.exception.UserServiceCallException;
import com.von.orderservice.map.AmapException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * order-service 全局异常处理器。
 * <p>
 * 按 errorCode 区分不同失败类型，统一返回 JSON，便于 Postman/前端/Gateway 识别：
 * <ul>
 *   <li>AMAP_ERROR — 高德地图相关（400）</li>
 *   <li>INSUFFICIENT_BALANCE — 余额不足（400）</li>
 *   <li>SENTINEL_BLOCKED — Sentinel 限流（429）</li>
 *   <li>USER_SERVICE_ERROR — 远程 user-service 其他错误（502）</li>
 *   <li>INTERNAL_ERROR — 未预期内部错误（500）</li>
 * </ul>
 * </p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(com.von.orderservice.exception.TripException.class)
    public ResponseEntity<Map<String, Object>> handleTripException(com.von.orderservice.exception.TripException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseError(e.getErrorCode(), e.getMessage()));
    }

    /** 高德 API Key 未配置、地址解析失败等 */
    @ExceptionHandler(AmapException.class)
    public ResponseEntity<Map<String, Object>> handleAmapException(AmapException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(baseError("AMAP_ERROR", e.getMessage()));
    }

    /**
     * 余额不足（通常来自 Feign 扣款失败，或 UserFeignExceptionTranslator 翻译结果）。
     * 返回 userId、balance、requiredAmount 方便展示「还差多少钱」。
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException e) {
        Map<String, Object> body = baseError(e.getErrorCode(), e.getMessage());
        body.put("passengerId", e.getUserId());
        body.put("balance", e.getBalance());
        body.put("requiredAmount", e.getRequiredAmount());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Sentinel 限流：createOrder 或 user-service deduct 被限流。
     * HTTP 429，与余额不足（400）明确区分。
     */
    @ExceptionHandler(UserServiceBlockedException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceBlocked(UserServiceBlockedException e) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(baseError(e.getErrorCode(), e.getMessage()));
    }

    /**
     * user-service 不可用、超时、500 等（非余额、非限流）。
     * HTTP 502 表示「上游服务出问题」。
     */
    @ExceptionHandler(UserServiceCallException.class)
    public ResponseEntity<Map<String, Object>> handleUserServiceCall(UserServiceCallException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(baseError(e.getErrorCode(), e.getMessage()));
    }

    /** 兜底：未分类异常返回 500 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(baseError("INTERNAL_ERROR", e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
    }

    /** 构造统一 JSON 错误体：success + errorCode + message */
    private Map<String, Object> baseError(String errorCode, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", "false");
        body.put("errorCode", errorCode);
        body.put("message", message);
        return body;
    }
}
