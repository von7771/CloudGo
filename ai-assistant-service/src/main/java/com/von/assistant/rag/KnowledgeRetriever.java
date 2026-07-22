package com.von.assistant.rag;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * LangChain 风格 RAG Retriever：关键词打分 + Top-K，不调用 Embedding API。
 */
@Component
public class KnowledgeRetriever {

    private static final List<KnowledgeChunk> KB = List.of(
            new KnowledgeChunk("carpool", "拼车模式",
                    "发单时选择「拼车」可享约 85 折。系统按路线相似度自动匹配拼友，不必起点终点完全相同，终点相近、方向一致即可入同一拼车池。满 2 人自动派单。",
                    new String[]{"拼车", "拼友", "折扣", "85", "路线"}),
            new KnowledgeChunk("solo", "独享模式",
                    "独享即一对一叫车，发单后立即进入派单，司机抢单接单。",
                    new String[]{"独享", "一对一", "叫车"}),
            new KnowledgeChunk("smart", "智能拼车推荐",
                    "司机工作台和 AI 助手可查看「智能拼车包」：系统将最多 3 笔同方向待接订单打包推荐，一键可接多单。算法基于经纬度与行驶方向，不消耗大模型 Token。",
                    new String[]{"智能", "推荐", "打包", "三单", "司机", "bundle"}),
            new KnowledgeChunk("pay", "支付与扣款",
                    "发单时校验余额与信用分，完单时才扣乘客余额并入账司机。余额不足完单会回滚。",
                    new String[]{"支付", "扣款", "余额", "信用", "完单"}),
            new KnowledgeChunk("cancel", "取消规则",
                    "乘客可在 CREATED、DISPATCHING、POOL_WAITING 状态取消。已接单后不可随意取消。",
                    new String[]{"取消", "退单"}),
            new KnowledgeChunk("driver", "司机上线",
                    "司机需先上线接单，系统每 60 秒上报位置。新单通过 WebSocket 实时推送，并有 10 秒轮询兜底。",
                    new String[]{"司机", "上线", "位置", "websocket", "轮询"}),
            new KnowledgeChunk("ai", "AI 助手",
                    "AI 助手可查询行程、解释拼车规则、为司机推荐智能拼车包。简单问答优先走知识库，复杂操作走 Function Calling。",
                    new String[]{"ai", "助手", "大模型", "deepseek"})
    );

    public String retrieveTop1(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        String q = query.toLowerCase(Locale.ROOT);
        return KB.stream()
                .map(chunk -> new Scored(chunk, score(chunk, q)))
                .filter(s -> s.score > 0)
                .max(Comparator.comparingInt(s -> s.score))
                .map(s -> "【" + s.chunk.title() + "】" + s.chunk.content())
                .orElse("");
    }

    public String search(String query, int topK) {
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT);
        return KB.stream()
                .map(chunk -> new Scored(chunk, score(chunk, q)))
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt((Scored s) -> s.score).reversed())
                .limit(topK)
                .map(s -> "- " + s.chunk.title() + ": " + s.chunk.content())
                .reduce((a, b) -> a + "\n" + b)
                .orElse("暂无匹配帮助文档。");
    }

    private int score(KnowledgeChunk chunk, String q) {
        int s = 0;
        if (q.contains(chunk.id())) {
            s += 3;
        }
        for (String kw : chunk.keywords()) {
            if (q.contains(kw.toLowerCase(Locale.ROOT))) {
                s += 2;
            }
        }
        for (String word : q.split("\\s+")) {
            if (word.length() >= 2 && chunk.content().contains(word)) {
                s += 1;
            }
        }
        return s;
    }

    private record Scored(KnowledgeChunk chunk, int score) {
    }
}
