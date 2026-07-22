package com.von.common.enums;

/**
 * 系统角色，对应 JWT 中的 role 声明与 Gateway 路由鉴权。
 */
public enum UserRole {

    /** 乘客：发单、支付、评价 */
    PASSENGER,

    /** 司机：上线、接单、完单 */
    DRIVER,

    /** 管理员：审核、监控、配置（P4 起完善能力，P0 仅预留） */
    ADMIN
}
