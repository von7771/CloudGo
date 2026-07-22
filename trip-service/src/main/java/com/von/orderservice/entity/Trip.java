package com.von.orderservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_trip")
public class Trip {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long passengerId;
    private String tripMode;
    private Long poolId;
    private Long driverId;
    private String startPoint;
    private String endPoint;
    private String startLocation;
    private String endLocation;
    private String status;
    private BigDecimal estimatedAmount;
    private BigDecimal finalAmount;
    private Integer distanceMeters;
    private Integer durationSeconds;
    private Integer passengerRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
