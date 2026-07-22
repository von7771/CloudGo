package com.von.assistant.dto;

import java.util.List;

public record ChatResponse(
        String reply,
        String sessionId,
        List<String> toolUsed,
        boolean fromCache
) {
}
