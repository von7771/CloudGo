package com.von.orderservice.map.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * 对外暴露的「驾车路线 + 预估费用」结果 DTO。
 * <p>
 * 包含起终点地址、经纬度、里程、时长，以及我们自己算出来的车费。
 * 下单接口 {@link com.von.orderservice.service.OrderService} 也会使用此对象。
 * </p>
 */
public record DrivingRouteResult(
        /** 起点标准化地址（来自地理编码） */
        String originAddress,
        /** 终点标准化地址（来自地理编码） */
        String destinationAddress,
        /** 起点经纬度，格式 "经度,纬度" */
        String originLocation,
        /** 终点经纬度，格式 "经度,纬度" */
        String destinationLocation,
        /** 路线总距离，单位：米 */
        int distanceMeters,
        /** 预估行驶时间，单位：秒 */
        int durationSeconds,
        /** 里程基础价（未计天气） */
        BigDecimal baseFare,
        /** 天气调价后预估车费（元） */
        BigDecimal estimatedFare,
        /** 天气类型，如 Rain / Clear */
        String weatherMain,
        /** 天气描述 */
        String weatherDescription,
        /** 气温（摄氏度） */
        Double temperatureCelsius,
        /** 天气价格倍率 */
        BigDecimal weatherMultiplier,
        /** 天气附加费 = estimatedFare - baseFare */
        BigDecimal weatherSurcharge,
        /** 驾车路线折线点（来自高德 steps.polyline，供地图绘制真实道路） */
        List<RoutePoint> routePoints
) {
}
