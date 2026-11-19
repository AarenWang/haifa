package org.wrj.haifa.ai.deerflow.model.cache;

import java.util.Map;

public record ModelUsage(
        Long inputTokens,
        Long uncachedInputTokens,
        Long outputTokens,
        Long totalTokens,
        Long cacheReadInputTokens,
        Long cacheWriteInputTokens,
        Long reasoningTokens,
        String provider,
        String model,
        UsageAvailability availability,
        Map<String, Long> providerDetails) {

    public static ModelUsage empty() {
        return new ModelUsage(
                null, null, null, null, null, null, null,
                "", "", UsageAvailability.UNAVAILABLE, Map.of());
    }

    public Double cacheHitRate() {
        if (inputTokens == null || inputTokens <= 0 || cacheReadInputTokens == null) {
            return null;
        }
        return Math.min(1.0d, (double) cacheReadInputTokens / inputTokens);
    }
}
