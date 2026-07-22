package com.von.orderservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_carpool_pool")
public class CarpoolPool {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String status;
    private String endPointKey;
    private String endPoint;
    private Integer maxSeats;
    private Integer currentSeats;
    private LocalDateTime createdAt;
}
