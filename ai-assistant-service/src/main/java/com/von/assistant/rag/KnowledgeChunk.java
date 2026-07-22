package com.von.assistant.rag;

/**
 * 知识库片段（轻量 RAG，本地关键词检索，零 Embedding 费用）。
 */
public record KnowledgeChunk(
        String id,
        String title,
        String content,
        String[] keywords
) {
}
