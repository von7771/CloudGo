package com.von.orderservice.map.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * 高德「地理编码」API 的 JSON 响应映射类（内部使用，不直接暴露给前端）。
 * <p>
 * 对应接口：GET /v3/geocode/geo
 * RestClient 收到 JSON 后，Jackson 按字段名自动反序列化到这个 record。
 * </p>
 */
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略 JSON 里我们没定义的字段，避免高德新增字段导致反序列化失败
public record AmapGeocodeResponse(
        /** 高德业务状态码，"1" 表示成功，其他值表示失败 */
        String status,
        /** 失败时的说明文字，如 "INVALID_USER_KEY" */
        String info,
        /** 解析结果列表，通常取第一个元素 */
        List<GeocodeItem> geocodes
) {
    /**
     * 单条地理编码结果。
     * 字段名 formatted_address 与高德 JSON 保持一致（下划线命名）。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GeocodeItem(
            /** 经纬度，格式 "经度,纬度"，如 "116.397463,39.909187" */
            String location,
            /** 高德标准化后的完整地址文字 */
            String formatted_address
    ) {
    }
}
