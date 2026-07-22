package com.von.common.enums;

/**
 * 行程状态机（P1 起在 trip-service 使用）。
 * <p>
 * 流转：CREATED → DISPATCHING → ACCEPTED → ARRIVED → IN_PROGRESS → COMPLETED
 * 任意未完成阶段可 → CANCELLED（规则在 trip-service 实现）
 * </p>
 */
public enum TripStatus {

    /** 乘客已发单，待派单 */
    CREATED,

    /** 已推送给司机，等待接单 */
    DISPATCHING,

    /** 拼车等待拼友（满员后进入 DISPATCHING） */
    POOL_WAITING,

    /** 司机已接单 */
    ACCEPTED,

    /** 司机到达上车点 */
    ARRIVED,

    /** 行程进行中 */
    IN_PROGRESS,

    /** 已完单（P3 在此状态触发扣款） */
    COMPLETED,

    /** 已取消 */
    CANCELLED
}
