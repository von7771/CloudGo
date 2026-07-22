package com.von.orderservice.weather.dto;

import java.math.BigDecimal;

/** 对外暴露的天气 + 调价倍率 */
public record WeatherInfoDto(
        String main,
        String description,
        double temperatureCelsius,
        BigDecimal priceMultiplier
) {
}
