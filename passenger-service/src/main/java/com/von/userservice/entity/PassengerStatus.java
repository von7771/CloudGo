package com.von.userservice.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_passenger_status")
public class PassengerStatus {

    @TableId
    private Long passengerId;
    private String status;
}
