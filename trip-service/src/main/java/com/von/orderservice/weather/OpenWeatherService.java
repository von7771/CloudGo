package com.von.orderservice.weather;

import com.von.orderservice.config.OpenWeatherProperties;
import com.von.orderservice.weather.dto.OpenWeatherResponse;
import com.von.orderservice.weather.dto.WeatherSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Service
public class OpenWeatherService {

    private static final Logger log = LoggerFactory.getLogger(OpenWeatherService.class);

    private final OpenWeatherProperties properties;
    private final RestClient restClient;

    public OpenWeatherService(OpenWeatherProperties properties,
                              @Qualifier("openWeatherRestClient") RestClient openWeatherRestClient) {
        this.properties = properties;
        this.restClient = openWeatherRestClient;
    }

    /**
     * 根据经纬度查询当前天气。Key 未配置或调用失败时返回 empty，不影响发单。
     */
    public Optional<WeatherSnapshot> getCurrentWeather(double latitude, double longitude) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return Optional.empty();
        }
        try {
            OpenWeatherResponse response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/data/2.5/weather")
                            .queryParam("lat", latitude)
                            .queryParam("lon", longitude)
                            .queryParam("appid", properties.getApiKey())
                            .queryParam("units", "metric")
                            .queryParam("lang", "zh_cn")
                            .build())
                    .retrieve()
                    .body(OpenWeatherResponse.class);

            if (response == null || response.weather() == null || response.weather().isEmpty()) {
                return Optional.empty();
            }
            OpenWeatherResponse.WeatherItem item = response.weather().get(0);
            double temp = response.main() != null && response.main().temp() != null
                    ? response.main().temp()
                    : 0.0;
            return Optional.of(new WeatherSnapshot(item.main(), item.description(), temp));
        } catch (Exception ex) {
            log.warn("OpenWeather 查询失败 lat={}, lon={}: {}", latitude, longitude, ex.getMessage());
            return Optional.empty();
        }
    }

    /** 解析高德坐标串 "经度,纬度" */
    public Optional<WeatherSnapshot> getCurrentWeatherByLocation(String location) {
        if (!StringUtils.hasText(location) || !location.contains(",")) {
            return Optional.empty();
        }
        String[] parts = location.split(",");
        double longitude = Double.parseDouble(parts[0].trim());
        double latitude = Double.parseDouble(parts[1].trim());
        return getCurrentWeather(latitude, longitude);
    }
}
