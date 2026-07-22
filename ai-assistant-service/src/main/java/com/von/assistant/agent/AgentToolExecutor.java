package com.von.assistant.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.von.assistant.feign.TripFeignClient;
import com.von.assistant.rag.KnowledgeRetriever;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * LangChain Agent Tools：Function Calling 工具注册与执行（本地逻辑零 Token）。
 */
@Component
public class AgentToolExecutor {

    private final TripFeignClient tripFeignClient;
    private final KnowledgeRetriever knowledgeRetriever;
    private final ObjectMapper objectMapper;

    public AgentToolExecutor(TripFeignClient tripFeignClient,
                             KnowledgeRetriever knowledgeRetriever,
                             ObjectMapper objectMapper) {
        this.tripFeignClient = tripFeignClient;
        this.knowledgeRetriever = knowledgeRetriever;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> toolDefinitions() {
        return List.of(
                tool("search_help", "搜索拼车系统帮助文档（RAG），用于规则/流程类问题",
                        Map.of("query", prop("string", "用户问题关键词"))),
                tool("list_my_trips", "查询当前用户最近行程（乘客）",
                        Map.of("limit", prop("integer", "条数，默认5"))),
                tool("recommend_smart_carpool", "为司机推荐同路线打包订单（最多3单/包，纯算法+结构化结果）",
                        Map.of("limit", prop("integer", "推荐包数量，默认3"))),
                tool("suggest_carpool_for_trip", "为指定行程找可拼的同向订单",
                        Map.of("trip_id", prop("integer", "行程ID")))
        );
    }

    @SuppressWarnings("unchecked")
    public String execute(String name, String argsJson, Long userId, String role) {
        try {
            Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
            return switch (name) {
                case "search_help" -> knowledgeRetriever.search(String.valueOf(args.getOrDefault("query", "")), 2);
                case "list_my_trips" -> {
                    if (!"PASSENGER".equalsIgnoreCase(role)) {
                        yield "当前角色非乘客，无法查询乘客行程。";
                    }
                    int limit = intArg(args, "limit", 5);
                    yield toJson(tripFeignClient.passengerRecent(userId, limit));
                }
                case "recommend_smart_carpool" -> {
                    if (!"DRIVER".equalsIgnoreCase(role)) {
                        yield "该工具面向司机；乘客可问「拼车怎么选」。";
                    }
                    int limit = intArg(args, "limit", 3);
                    yield toJson(tripFeignClient.smartBundles(limit));
                }
                case "suggest_carpool_for_trip" -> {
                    long tripId = longArg(args, "trip_id");
                    yield toJson(tripFeignClient.smartBundlesForTrip(tripId, 3));
                }
                default -> "未知工具: " + name;
            };
        } catch (Exception e) {
            return "工具执行失败: " + e.getMessage();
        }
    }

    private Map<String, Object> tool(String name, String desc, Map<String, Object> properties) {
        Map<String, Object> fn = new LinkedHashMap<>();
        fn.put("name", name);
        fn.put("description", desc);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("type", "object");
        params.put("properties", properties);
        fn.put("parameters", params);

        Map<String, Object> tool = new LinkedHashMap<>();
        tool.put("type", "function");
        tool.put("function", fn);
        return tool;
    }

    private Map<String, Object> prop(String type, String desc) {
        return Map.of("type", type, "description", desc);
    }

    private int intArg(Map<String, Object> args, String key, int def) {
        Object v = args.get(key);
        if (v == null) {
            return def;
        }
        return v instanceof Number n ? n.intValue() : Integer.parseInt(String.valueOf(v));
    }

    private long longArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v == null) {
            throw new IllegalArgumentException("缺少参数 " + key);
        }
        return v instanceof Number n ? n.longValue() : Long.parseLong(String.valueOf(v));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }
}
