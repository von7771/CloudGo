package com.von.orderservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_pricing_rule")
public class PricingRule {

    @TableId
    private Long id;
    private BigDecimal baseFare;
    private BigDecimal perKmRate;
    private BigDecimal minFare;
    private LocalDateTime updatedAt;
}
