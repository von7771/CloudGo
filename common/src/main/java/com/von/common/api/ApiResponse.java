package com.von.common.api;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一 API 响应体。
 *
 * @param <T> data 字段类型
 */
public record ApiResponse<T>(
        boolean success,
        String errorCode,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, ApiErrorCode.SUCCESS, "OK", data);
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, ApiErrorCode.SUCCESS, message, data);
    }

    public static ApiResponse<Void> fail(String errorCode, String message) {
        return new ApiResponse<>(false, errorCode, message, null);
    }

    /**
     * 转为 Map，便于 GlobalExceptionHandler 追加 userId、balance 等扩展字段。
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("success", success ? "true" : "false");
        map.put("errorCode", errorCode);
        map.put("message", message);
        if (data != null) {
            map.put("data", data);
        }
        return map;
    }
}
