package com.von.common.api;

/**
 * 全项目统一错误码常量。
 */
public final class ApiErrorCode {

    private ApiErrorCode() {
    }

    public static final String SUCCESS = "SUCCESS";

    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";

    public static final String INSUFFICIENT_BALANCE = "INSUFFICIENT_BALANCE";
    public static final String SENTINEL_BLOCKED = "SENTINEL_BLOCKED";
    public static final String USER_SERVICE_ERROR = "USER_SERVICE_ERROR";
    public static final String AMAP_ERROR = "AMAP_ERROR";

    public static final String TRIP_STATUS_INVALID = "TRIP_STATUS_INVALID";
    public static final String DRIVER_NOT_AVAILABLE = "DRIVER_NOT_AVAILABLE";

    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
}
