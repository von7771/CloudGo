package com.von.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_passenger")
public class Passenger {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String avatarObject;
    private String password;
    private Integer creditScore;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
