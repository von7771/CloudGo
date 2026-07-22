package com.von.common.dto;

import java.math.BigDecimal;

/**
 * 计价规则（起步价 + 里程单价 + 最低消费）。
 */
public record PricingRuleDto(
        BigDecimal baseFare,
        BigDecimal perKmRate,
        BigDecimal minFare
) {
}
