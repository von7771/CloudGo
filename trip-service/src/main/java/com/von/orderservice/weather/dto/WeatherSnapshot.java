package com.von.orderservice.weather.dto;

/**
 * 某地当前天气快照（用于动态调价）。
 */
public record WeatherSnapshot(
        String main,
        String description,
        double temperatureCelsius
) {
}
