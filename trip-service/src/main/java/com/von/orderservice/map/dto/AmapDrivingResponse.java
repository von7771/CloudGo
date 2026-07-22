package com.von.orderservice.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 高德「驾车路径规划」API 的 JSON 响应映射类（内部使用）。
 * <p>
 * 对应接口：GET /v3/direction/driving
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略多余 JSON 字段，保证兼容性
public record AmapDrivingResponse(
        /** 高德业务状态码，"1" 表示成功 */
        String status,
        /** 失败时的说明文字 */
        String info,
        /** 路线信息容器 */
        Route route
) {
    /** 路线节点，包含多条可选路径 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Route(
            /** 路径列表，demo 中取第一条 paths[0] */
            List<Path> paths
    ) {
    }

    /** 单条路径的距离、时长与分段坐标 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Path(
            /** 路线总距离，单位：米，如 "11234" */
            String distance,
            /** 预估行驶时间，单位：秒，如 "1800" */
            String duration,
            /** 分段导航，每段含 polyline 坐标串 */
            List<Step> steps
    ) {
    }

    /** 导航分段，polyline 格式：经度,纬度;经度,纬度 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Step(
            String polyline
    ) {
    }
}
