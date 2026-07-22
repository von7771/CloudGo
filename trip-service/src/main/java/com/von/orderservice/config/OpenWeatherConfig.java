package com.von.orderservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenWeatherProperties.class)
public class OpenWeatherConfig {

    @Bean("openWeatherRestClient")
    RestClient openWeatherRestClient() {
        return RestClient.builder()
                .baseUrl("https://api.openweathermap.org")
                .build();
    }
}
