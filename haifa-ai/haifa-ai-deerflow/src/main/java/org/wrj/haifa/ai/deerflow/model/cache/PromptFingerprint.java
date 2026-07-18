package org.wrj.haifa.ai.deerflow.model.cache;

public record PromptFingerprint(
        String schemaVersion,
        String staticSystemHash,
        String toolDefinitionsHash,
        String skillCatalogHash,
        String activeSkillsHash,
        String sessionContextHash,
        String conversationPrefixHash,
        String dynamicTailHash,
        String cacheablePrefixHash,
        int estimatedInputTokens,
        int estimatedCacheablePrefixTokens) {

    public static PromptFingerprint empty() {
        return new PromptFingerprint("v1", "", "", "", "", "", "", "", "", 0, 0);
    }
}
