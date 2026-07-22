package com.von.driver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_driver")
public class Driver {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String nickname;
    private String avatarObject;
    private String password;
    private String realName;
    private String auditStatus;
    private String licenseImageObject;
    private String idCardImageObject;
    private BigDecimal balance;
    private LocalDateTime createdAt;
}
