package org.wrj.haifa.ai.deerflow.model.cache;

public record PromptBlock(
        PromptBlockType type,
        PromptBlockPlacement placement,
        PromptStability stability,
        PromptCacheability cacheability,
        String version,
        String content) {
}
