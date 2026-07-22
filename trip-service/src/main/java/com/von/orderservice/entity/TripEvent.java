package com.von.orderservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_trip_event")
public class TripEvent {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long tripId;
    private String fromStatus;
    private String toStatus;
    private String operator;
    private String remark;
    private LocalDateTime createdAt;
}
