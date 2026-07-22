package com.von.orderservice.service;

import com.von.common.dto.PricingRuleDto;
import com.von.orderservice.entity.PricingRule;
import com.von.orderservice.exception.TripException;
import com.von.orderservice.mapper.PricingRuleMapper;
import com.von.orderservice.weather.WeatherPricingCalculator;
import com.von.orderservice.weather.dto.WeatherSnapshot;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class PricingService {

    private static final long RULE_ID = 1L;

    private final PricingRuleMapper pricingRuleMapper;
    private final WeatherPricingCalculator weatherPricingCalculator;

    public PricingService(PricingRuleMapper pricingRuleMapper,
                          WeatherPricingCalculator weatherPricingCalculator) {
        this.pricingRuleMapper = pricingRuleMapper;
        this.weatherPricingCalculator = weatherPricingCalculator;
    }

    public PricingRuleDto getRule() {
        PricingRule rule = requireRule();
        return toDto(rule);
    }

    public PricingRuleDto updateRule(BigDecimal baseFare, BigDecimal perKmRate, BigDecimal minFare) {
        validate(baseFare, perKmRate, minFare);
        PricingRule rule = requireRule();
        rule.setBaseFare(baseFare);
        rule.setPerKmRate(perKmRate);
        rule.setMinFare(minFare);
        rule.setUpdatedAt(LocalDateTime.now());
        pricingRuleMapper.updateById(rule);
        return toDto(rule);
    }

    public BigDecimal estimateFare(int distanceMeters) {
        return estimateBaseFare(distanceMeters);
    }

    public BigDecimal estimateBaseFare(int distanceMeters) {
        PricingRule rule = requireRule();
        BigDecimal distanceKm = BigDecimal.valueOf(distanceMeters)
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);
        BigDecimal fare = rule.getBaseFare()
                .add(distanceKm.multiply(rule.getPerKmRate()))
                .setScale(2, RoundingMode.HALF_UP);
        return fare.max(rule.getMinFare());
    }

    /**
     * 在基础价上应用天气倍率。
     */
    public WeatherAdjustedFare applyWeather(BigDecimal baseFare, WeatherSnapshot weather) {
        BigDecimal multiplier = weatherPricingCalculator.multiplier(weather);
        BigDecimal finalFare = baseFare.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
        BigDecimal surcharge = finalFare.subtract(baseFare).setScale(2, RoundingMode.HALF_UP);
        return new WeatherAdjustedFare(baseFare, finalFare, multiplier, surcharge);
    }

    public record WeatherAdjustedFare(
            BigDecimal baseFare,
            BigDecimal finalFare,
            BigDecimal multiplier,
            BigDecimal surcharge
    ) {
    }

    private PricingRule requireRule() {
        PricingRule rule = pricingRuleMapper.selectById(RULE_ID);
        if (rule == null) {
            throw new TripException("计价规则未初始化，请执行 deploy/sql/schema.sql");
        }
        return rule;
    }

    private static void validate(BigDecimal baseFare, BigDecimal perKmRate, BigDecimal minFare) {
        if (baseFare == null || perKmRate == null || minFare == null) {
            throw new TripException("计价参数不能为空");
        }
        if (baseFare.signum() < 0 || perKmRate.signum() < 0 || minFare.signum() <= 0) {
            throw new TripException("计价参数不合法");
        }
    }

    private static PricingRuleDto toDto(PricingRule rule) {
        return new PricingRuleDto(rule.getBaseFare(), rule.getPerKmRate(), rule.getMinFare());
    }
}
