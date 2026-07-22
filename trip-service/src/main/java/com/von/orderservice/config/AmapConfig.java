package com.von.orderservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * 高德地图相关 Spring 配置类。
 * <p>
 * 负责两件事：1）注册 {@link AmapProperties}；2）创建调用高德 REST API 的 HTTP 客户端。
 * </p>
 */
@Configuration // 声明这是一个 Spring 配置类，启动时会被扫描加载
@EnableConfigurationProperties(AmapProperties.class) // 启用类型安全配置绑定，把 yaml 映射到 AmapProperties
public class AmapConfig {

    /**
     * 创建专用于高德 API 的 RestClient Bean。
     * <p>
     * RestClient 是 Spring 6+ 推荐的 HTTP 客户端，用来发 GET/POST 请求。
     * 这里预先设置 baseUrl，后续请求只需写路径（如 /v3/geocode/geo）。
     * </p>
     *
     * @return 指向 https://restapi.amap.com 的 HTTP 客户端实例
     */
    @Bean("amapRestClient")
    RestClient amapRestClient() {
        return RestClient.builder() // 构建 RestClient 实例
                .baseUrl("https://restapi.amap.com") // 高德 Web 服务 API 的根地址
                .build(); // 完成构建并返回
    }
}
