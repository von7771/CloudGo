package com.von.assistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.von.assistant.agent.AgentToolExecutor;
import com.von.assistant.config.LlmProperties;
import com.von.assistant.dto.ChatRequest;
import com.von.assistant.dto.ChatResponse;
import com.von.assistant.feign.TripFeignClient;
import com.von.assistant.llm.DeepSeekClient;
import com.von.assistant.rag.KnowledgeRetriever;
import com.von.common.dto.SmartCarpoolBundleDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LangChain 风格 Agent Orchestrator：RAG 预检索 + Function Calling（最多 2 轮，控 Token）。
 */
@Service
public class AssistantChatService {

    private static final String SYSTEM_PROMPT = """
            你是拼车出行小程序助手。回答要简短（不超过120字），用中文。
            涉及订单/推荐时优先调用工具，不要编造订单号。
            拼车匹配已用路线相似度算法，你负责解释结果并给操作建议。
            """;

    private final DeepSeekClient deepSeekClient;
    private final AgentToolExecutor toolExecutor;
    private final KnowledgeRetriever knowledgeRetriever;
    private final TripFeignClient tripFeignClient;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;
    private final Map<String, List<Map<String, Object>>> sessions = new ConcurrentHashMap<>();

    public AssistantChatService(DeepSeekClient deepSeekClient,
                                AgentToolExecutor toolExecutor,
                                KnowledgeRetriever knowledgeRetriever,
                                TripFeignClient tripFeignClient,
                                LlmProperties llmProperties,
                                ObjectMapper objectMapper) {
        this.deepSeekClient = deepSeekClient;
        this.toolExecutor = toolExecutor;
        this.knowledgeRetriever = knowledgeRetriever;
        this.tripFeignClient = tripFeignClient;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public ChatResponse chat(Long userId, String role, ChatRequest req) {
        String message = req.message() == null ? "" : req.message().trim();
        if (message.isBlank()) {
            return new ChatResponse("请输入问题～", null, List.of(), false);
        }

        String sessionId = resolveSessionId(req);
        List<String> toolsUsed = new ArrayList<>();

        // 快捷指令：零 Token 直出
        String quick = tryQuickReply(message, userId, role, toolsUsed);
        if (quick != null) {
            appendHistory(sessionId, "user", message);
            appendHistory(sessionId, "assistant", quick);
            return new ChatResponse(quick, sessionId, toolsUsed, true);
        }

        String ragHint = knowledgeRetriever.retrieveTop1(message);
        List<Map<String, Object>> messages = buildMessages(sessionId, message, role, req.context(), ragHint);

        DeepSeekClient.ChatResult result = deepSeekClient.chat(messages, toolExecutor.toolDefinitions());
        int rounds = 0;

        while (result.toolCalls() != null && !result.toolCalls().isEmpty()
                && rounds < llmProperties.maxToolRounds()) {
            rounds++;
            Map<String, Object> assistantMsg = new LinkedHashMap<>();
            assistantMsg.put("role", "assistant");
            assistantMsg.put("content", result.content() == null ? "" : result.content());
            assistantMsg.put("tool_calls", result.toolCalls().stream().map(tc -> {
                Map<String, Object> call = new LinkedHashMap<>();
                call.put("id", tc.id());
                call.put("type", "function");
                Map<String, Object> fn = new LinkedHashMap<>();
                fn.put("name", tc.name());
                fn.put("arguments", tc.argumentsJson());
                call.put("function", fn);
                return call;
            }).toList());
            messages.add(assistantMsg);

            for (DeepSeekClient.ToolCall tc : result.toolCalls()) {
                toolsUsed.add(tc.name());
                String toolResult = toolExecutor.execute(tc.name(), tc.argumentsJson(), userId, role);
                messages.add(Map.of(
                        "role", "tool",
                        "tool_call_id", tc.id(),
                        "content", truncate(toolResult, 1500)
                ));
            }

            result = deepSeekClient.chat(messages, null);
        }

        String reply = result.content();
        if (reply == null || reply.isBlank()) {
            reply = "我已查询相关信息，如需更详细说明请换个问法。";
        }

        appendHistory(sessionId, "user", message);
        appendHistory(sessionId, "assistant", reply);
        return new ChatResponse(reply, sessionId, toolsUsed, false);
    }

    public List<SmartCarpoolBundleDto> smartBundlesDirect(int limit) {
        return tripFeignClient.smartBundles(limit);
    }

    private String tryQuickReply(String message, Long userId, String role, List<String> toolsUsed) {
        String m = message.toLowerCase();
        if ("DRIVER".equalsIgnoreCase(role) && (m.contains("智能拼车") || (m.contains("推荐") && m.contains("司机")))) {
            toolsUsed.add("recommend_smart_carpool");
            List<SmartCarpoolBundleDto> bundles = tripFeignClient.smartBundles(3);
            if (bundles.isEmpty()) {
                return "暂无智能拼车包。请保持上线，有多笔同向独享待接订单时会自动推荐（最多3单/包）。";
            }
            StringBuilder sb = new StringBuilder("智能拼车推荐：\n");
            for (SmartCarpoolBundleDto b : bundles) {
                sb.append("• ").append(b.summary()).append("，合计 ¥").append(b.totalEstimatedFare()).append("\n");
                b.trips().forEach(t -> sb.append("  - #").append(t.tripId()).append(" ")
                        .append(t.startPoint()).append(" → ").append(t.endPoint()).append("\n"));
            }
            sb.append("可在工作台一键接单。");
            return sb.toString().trim();
        }
        if ("PASSENGER".equalsIgnoreCase(role) && m.contains("查") && m.contains("行程")) {
            toolsUsed.add("list_my_trips");
            return tripFeignClient.passengerRecent(userId, 5).toString();
        }
        if (m.contains("拼车") && (m.contains("怎么") || m.contains("如何") || m.contains("用"))) {
            toolsUsed.add("search_help");
            return knowledgeRetriever.search("拼车", 2);
        }
        if (m.contains("帮助") || m.contains("怎么用") || m.contains("规则")) {
            toolsUsed.add("search_help");
            return knowledgeRetriever.search(message, 2);
        }
        return null;
    }

    private List<Map<String, Object>> buildMessages(String sessionId, String message, String role,
                                                    Map<String, Object> context, String ragHint) {
        List<Map<String, Object>> messages = new ArrayList<>();
        StringBuilder sys = new StringBuilder(SYSTEM_PROMPT);
        sys.append("\n用户角色:").append(role);
        if (ragHint != null && !ragHint.isBlank()) {
            sys.append("\n参考知识:").append(ragHint);
        }
        if (context != null && !context.isEmpty()) {
            sys.append("\n页面上下文:").append(context);
        }
        messages.add(Map.of("role", "system", "content", sys.toString()));

        List<Map<String, Object>> history = sessions.getOrDefault(sessionId, List.of());
        int from = Math.max(0, history.size() - 4);
        messages.addAll(history.subList(from, history.size()));
        messages.add(Map.of("role", "user", "content", message));
        return messages;
    }

    private String resolveSessionId(ChatRequest req) {
        if (req.context() != null && req.context().get("sessionId") != null) {
            return String.valueOf(req.context().get("sessionId"));
        }
        return UUID.randomUUID().toString().substring(0, 12);
    }

    private void appendHistory(String sessionId, String role, String content) {
        sessions.computeIfAbsent(sessionId, k -> new ArrayList<>())
                .add(new LinkedHashMap<>(Map.of("role", role, "content", content)));
        List<Map<String, Object>> list = sessions.get(sessionId);
        if (list.size() > 8) {
            sessions.put(sessionId, new ArrayList<>(list.subList(list.size() - 8, list.size())));
        }
    }

    private String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }
}
