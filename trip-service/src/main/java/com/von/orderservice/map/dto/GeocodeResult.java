package com.von.orderservice.map.dto;

/**
 * 对外暴露的「地理编码」结果 DTO。
 * <p>
 * 与 {@link AmapGeocodeResponse} 的区别：字段是我们业务语义，命名更友好，
 * Controller 直接 return 此对象，Spring 自动序列化为 JSON 返回给 Postman/前端。
 * </p>
 */
public record GeocodeResult(
        /** 用户传入的原始地址 */
        String address,
        /** 解析后的经纬度，格式 "经度,纬度" */
        String location,
        /** 高德标准化后的地址（用于展示和入库） */
        String formattedAddress
) {
}
