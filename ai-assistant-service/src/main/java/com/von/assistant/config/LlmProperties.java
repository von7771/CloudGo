package com.von.assistant.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.llm")
public record LlmProperties(
        String baseUrl,
        String model,
        String apiKey,
        int maxTokens,
        double temperature,
        int maxToolRounds
) {
}
