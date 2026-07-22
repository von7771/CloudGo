package com.von.orderservice.weather.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OpenWeatherResponse(
        List<WeatherItem> weather,
        MainBlock main
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record WeatherItem(String main, String description) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record MainBlock(Double temp) {
    }
}
