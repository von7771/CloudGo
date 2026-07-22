package com.von.assistant.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.von.assistant.config.LlmProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * DeepSeek OpenAI 兼容 Chat Completions + Function Calling。
 */
@Component
public class DeepSeekClient {

    private final RestClient restClient;
    private final LlmProperties props;
    private final ObjectMapper objectMapper;

    public DeepSeekClient(RestClient deepSeekRestClient, LlmProperties props, ObjectMapper objectMapper) {
        this.restClient = deepSeekRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    public ChatResult chat(List<Map<String, Object>> messages, List<Map<String, Object>> tools) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException("未配置 DEEPSEEK_API_KEY");
        }

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", props.model());
        body.put("max_tokens", props.maxTokens());
        body.put("temperature", props.temperature());
        body.set("messages", objectMapper.valueToTree(messages));
        if (tools != null && !tools.isEmpty()) {
            body.set("tools", objectMapper.valueToTree(tools));
            body.put("tool_choice", "auto");
        }

        String response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + props.apiKey())
                .body(body.toString())
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choice = root.path("choices").path(0).path("message");
            String content = choice.path("content").asText(null);
            List<ToolCall> toolCalls = parseToolCalls(choice.path("tool_calls"));
            return new ChatResult(content, toolCalls);
        } catch (Exception e) {
            throw new IllegalStateException("解析 LLM 响应失败: " + e.getMessage());
        }
    }

    private List<ToolCall> parseToolCalls(JsonNode arr) {
        List<ToolCall> list = new ArrayList<>();
        if (!arr.isArray()) {
            return list;
        }
        for (JsonNode node : arr) {
            list.add(new ToolCall(
                    node.path("id").asText(),
                    node.path("function").path("name").asText(),
                    node.path("function").path("arguments").asText("{}")
            ));
        }
        return list;
    }

    public record ToolCall(String id, String name, String argumentsJson) {
    }

    public record ChatResult(String content, List<ToolCall> toolCalls) {
    }
}
