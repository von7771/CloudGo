package com.von.orderservice.weather;

import com.von.orderservice.weather.dto.WeatherSnapshot;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 根据 OpenWeather 天气类型计算价格倍率。
 */
@Component
public class WeatherPricingCalculator {

    private static final BigDecimal MAX_MULTIPLIER = new BigDecimal("1.50");

    public BigDecimal multiplier(WeatherSnapshot weather) {
        if (weather == null) {
            return BigDecimal.ONE;
        }
        BigDecimal factor = baseFactorByMain(weather.main());
        factor = applyTemperatureAdjustment(factor, weather.temperatureCelsius());
        if (factor.compareTo(MAX_MULTIPLIER) > 0) {
            return MAX_MULTIPLIER;
        }
        return factor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal baseFactorByMain(String main) {
        if (main == null) {
            return BigDecimal.ONE;
        }
        switch (main.toUpperCase()) {
            case "THUNDERSTORM":
                return new BigDecimal("1.25");
            case "RAIN":
                return new BigDecimal("1.15");
            case "DRIZZLE":
                return new BigDecimal("1.10");
            case "SNOW":
                return new BigDecimal("1.30");
            case "MIST", "FOG", "HAZE", "SMOKE", "DUST", "SAND", "ASH":
                return new BigDecimal("1.10");
            case "EXTREME", "SQUALL", "TORNADO":
                return new BigDecimal("1.35");
            default:
                return BigDecimal.ONE;
        }
    }

    private BigDecimal applyTemperatureAdjustment(BigDecimal factor, double temperatureCelsius) {
        BigDecimal result = factor;
        if (temperatureCelsius >= 35) {
            result = result.multiply(new BigDecimal("1.05"));
        }
        if (temperatureCelsius <= 0) {
            result = result.multiply(new BigDecimal("1.10"));
        }
        return result.setScale(2, RoundingMode.HALF_UP);
    }
}
