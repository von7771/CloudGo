package com.von.orderservice.map.dto;

/**
 * 地图折线坐标点（纬度、经度），供小程序 map 组件 polyline 绘制。
 */
public record RoutePoint(
        double latitude,
        double longitude
) {
}
