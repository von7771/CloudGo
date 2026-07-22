package com.von.orderservice.feign;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.von.orderservice.exception.InsufficientBalanceException;
import com.von.orderservice.exception.UserServiceBlockedException;
import com.von.orderservice.exception.UserServiceCallException;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Feign 远程调用失败时的异常翻译器。
 * <p>
 * OpenFeign 在 user-service 返回 4xx/5xx 或超时熔断时会触发 {@link UserFeignFallbackFactory}，
 * 本类负责解析 FeignException 中的 HTTP 状态码和 JSON 响应体，
 * 将其转换为 order-service 可识别的三类业务异常，避免笼统的「Sentinel/Feign 降级」提示。
 * </p>
 */
@Component
public class UserFeignExceptionTranslator {

    /**
     * 本地 JSON 解析器。
     * Spring Boot 4 默认不再向容器注册 com.fasterxml.jackson.databind.ObjectMapper Bean，
     * 因此这里直接 new，避免启动时注入失败。
     */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 入口：根据底层 cause 类型选择翻译策略。
     *
     * @param operation 操作描述，如「用户扣款」「查询用户信用分」
     * @param cause       Feign/Sentinel 传递过来的原始异常
     * @return 翻译后的业务异常（抛出后由 GlobalExceptionHandler 处理）
     */
    public RuntimeException translate(String operation, Throwable cause) {
        if (cause instanceof FeignException feignException) {
            // 绝大多数远程失败是 FeignException，可读取 status 和 response body
            return translateFeignException(operation, feignException);
        }
        // 非 Feign 异常时，仅根据 message 关键字做兜底分类
        return translateByMessage(operation, cause);
    }

    /**
     * 解析 FeignException：读取 HTTP 状态码 + user-service 返回的 JSON errorCode。
     */
    private RuntimeException translateFeignException(String operation, FeignException feignException) {
        String body = feignException.contentUTF8(); // 响应体原文（JSON 字符串）
        JsonNode json = parseJson(body);            // 尝试解析为 JSON

        String errorCode = text(json, "errorCode"); // user-service 写入的错误码
        String message = text(json, "message");     // user-service 写入的错误描述

        // ① 余额不足：errorCode 匹配，或 HTTP 400 且 body 含 INSUFFICIENT_BALANCE
        if (InsufficientBalanceException.ERROR_CODE.equals(errorCode)
                || (feignException.status() == 400 && body.contains(InsufficientBalanceException.ERROR_CODE))) {
            return new InsufficientBalanceException(
                    longVal(json, "passengerId", "userId"),
                    decimalVal(json, "balance"),
                    decimalVal(json, "requiredAmount"),
                    message != null ? message : "余额不足，无法完成扣款"
            );
        }

        // ② Sentinel 限流：errorCode=SENTINEL_BLOCKED，或 HTTP 429，或文案含 Sentinel 关键字
        if (UserServiceBlockedException.ERROR_CODE.equals(errorCode)
                || feignException.status() == 429
                || containsSentinelHint(body, message, feignException.getMessage())) {
            return new UserServiceBlockedException(
                    message != null ? message : operation + "被 Sentinel 限流，请稍后重试"
            );
        }

        // ③ 其他远程错误：超时、500、网络异常等
        return new UserServiceCallException(
                operation + "失败（HTTP " + feignException.status() + "）: "
                        + (message != null ? message : feignException.getMessage())
        );
    }

    /**
     * 非 FeignException 时的兜底翻译（仅根据异常 message 关键字判断）。
     */
    private RuntimeException translateByMessage(String operation, Throwable cause) {
        String message = cause.getMessage();
        if (containsSentinelHint(null, message, null)) {
            return new UserServiceBlockedException(message);
        }
        if (message != null && message.contains(InsufficientBalanceException.ERROR_CODE)) {
            return new InsufficientBalanceException(null, null, null, message);
        }
        return new UserServiceCallException(
                operation + "失败: " + (message != null ? message : cause.getClass().getSimpleName())
        );
    }

    /** 判断响应体/消息中是否包含 Sentinel 限流相关关键字 */
    private boolean containsSentinelHint(String body, String message, String feignMessage) {
        return containsAny(body, "SENTINEL_BLOCKED", "Sentinel 限流")
                || containsAny(message, "SENTINEL_BLOCKED", "Sentinel 限流")
                || containsAny(feignMessage, "SENTINEL_BLOCKED", "Sentinel 限流");
    }

    /** 检查 text 是否包含任意一个 keyword */
    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /** 将响应体字符串解析为 JsonNode；解析失败返回 null */
    private JsonNode parseJson(String body) {
        if (body == null || body.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return null; // 非 JSON 响应时不抛异常，走后续兜底逻辑
        }
    }

    /** 安全读取 JSON 字符串字段 */
    private String text(JsonNode json, String field) {
        if (json == null || !json.has(field) || json.get(field).isNull()) {
            return null;
        }
        return json.get(field).asText();
    }

    /** 安全读取 JSON 长整型字段，支持多个候选字段名 */
    private Long longVal(JsonNode json, String... fields) {
        if (json == null) {
            return null;
        }
        for (String field : fields) {
            if (json.has(field) && !json.get(field).isNull()) {
                return json.get(field).asLong();
            }
        }
        return null;
    }

    /** 安全读取 JSON 长整型字段（如 userId） */
    private Long longVal(JsonNode json, String field) {
        return longVal(json, new String[]{field});
    }

    /** 安全读取 JSON 金额字段，转为 BigDecimal */
    private BigDecimal decimalVal(JsonNode json, String field) {
        if (json == null || !json.has(field) || json.get(field).isNull()) {
            return null;
        }
        return new BigDecimal(json.get(field).asText());
    }
}
