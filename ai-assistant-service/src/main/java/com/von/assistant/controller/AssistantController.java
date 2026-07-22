package com.von.assistant.controller;

import com.von.assistant.dto.ChatRequest;
import com.von.assistant.dto.ChatResponse;
import com.von.assistant.service.AssistantChatService;
import com.von.common.api.ApiResponse;
import com.von.common.dto.SmartCarpoolBundleDto;
import com.von.common.security.JwtConstants;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {

    private final AssistantChatService assistantChatService;

    public AssistantController(AssistantChatService assistantChatService) {
        this.assistantChatService = assistantChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<ChatResponse> chat(
            @RequestHeader(JwtConstants.HEADER_USER_ID) Long userId,
            @RequestHeader(value = JwtConstants.HEADER_USER_ROLE, defaultValue = "PASSENGER") String role,
            @RequestBody ChatRequest request
    ) {
        return ApiResponse.ok(assistantChatService.chat(userId, role, request));
    }

    /** 零 Token：直接获取智能拼车包（仅司机） */
    @GetMapping("/smart-bundles")
    public ApiResponse<List<SmartCarpoolBundleDto>> smartBundles(
            @RequestHeader(value = JwtConstants.HEADER_USER_ROLE, defaultValue = "") String role,
            @RequestParam(value = "limit", defaultValue = "5") int limit
    ) {
        if (!"DRIVER".equalsIgnoreCase(role)) {
            throw new IllegalArgumentException("仅司机可查看智能拼车包");
        }
        return ApiResponse.ok(assistantChatService.smartBundlesDirect(limit));
    }
}
