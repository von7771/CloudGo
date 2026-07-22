package com.von.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 高德地图配置属性类。
 * <p>
 * Spring Boot 会自动把配置文件（Nacos / amap-local.yml）里以 {@code amap.} 开头的项
 * 绑定到这个 Java 对象的字段上，例如 {@code amap.api-key} → {@code apiKey}。
 * K8s 部署时优先使用环境变量 {@code AMAP_API_KEY}。
 * </p>
 */
@ConfigurationProperties(prefix = "amap") // 配置前缀，对应 yaml 中的 amap.xxx
public class AmapProperties {

    /**
     * 高德 Web 服务 API Key（密钥）。
     * 在 Nacos 的 order-service.yml 或本地 amap-local.yml 中配置，不要写死在代码里提交 Git。
     */
    private String apiKey;

    /**
     * 地理编码时的默认城市（如「北京」）。
     * 同名地址全国有很多，指定城市可提高解析准确度；留空则高德自行推断。
     */
    private String city = "";

    /** 优先读 K8s 环境变量，避免 Nacos 中未解析的 ${AMAP_API_KEY:} 占位符 */
    public String getApiKey() {
        String envKey = System.getenv("AMAP_API_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey.trim();
        }
        if (StringUtils.hasText(apiKey) && !apiKey.contains("${")) {
            return apiKey.trim();
        }
        return null;
    }

    /** Spring 通过反射调用，写入配置中的 amap.api-key */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /** Spring 通过反射调用，读取配置中的 amap.city */
    public String getCity() {
        return city;
    }

    /** Spring 通过反射调用，写入配置中的 amap.city */
    public void setCity(String city) {
        this.city = city;
    }
}
