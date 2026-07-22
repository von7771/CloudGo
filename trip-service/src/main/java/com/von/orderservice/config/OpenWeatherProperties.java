package com.von.orderservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "openweather")
public class OpenWeatherProperties {

    private boolean enabled = true;
    private String apiKey = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** 优先读环境变量 OPENWEATHER_API_KEY */
    public String getApiKey() {
        String envKey = System.getenv("OPENWEATHER_API_KEY");
        if (StringUtils.hasText(envKey)) {
            return envKey.trim();
        }
        if (StringUtils.hasText(apiKey) && !apiKey.contains("${")) {
            return apiKey.trim();
        }
        return null;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
