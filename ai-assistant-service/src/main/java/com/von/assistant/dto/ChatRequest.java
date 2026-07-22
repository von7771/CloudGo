package com.von.assistant.dto;

import java.util.Map;

public record ChatRequest(
        String message,
        Map<String, Object> context
) {
}
