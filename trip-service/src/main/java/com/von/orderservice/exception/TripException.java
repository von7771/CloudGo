package com.von.orderservice.exception;

import com.von.common.api.ApiErrorCode;

public class TripException extends RuntimeException {

    public TripException(String message) {
        super(message);
    }

    public String getErrorCode() {
        return ApiErrorCode.TRIP_STATUS_INVALID;
    }
}
