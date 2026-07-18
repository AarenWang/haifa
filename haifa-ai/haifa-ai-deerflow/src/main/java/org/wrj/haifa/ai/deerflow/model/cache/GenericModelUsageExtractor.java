package org.wrj.haifa.ai.deerflow.model.cache;

import java.util.Map;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;

public class GenericModelUsageExtractor implements ModelUsageExtractor {

    @Override
    public ModelUsage extractUsage(ChatResponseMetadata metadata, String requestedModel) {
        if (metadata == null || metadata.getUsage() == null) {
            return ModelUsage.empty();
        }
        Usage usage = metadata.getUsage();
        Long promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : null;
        Long completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : null;
        Long totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : null;

        if (promptTokens == null && completionTokens == null && totalTokens == null) {
            return ModelUsage.empty();
        }

        UsageAvailability availability = (promptTokens != null || completionTokens != null)
                ? UsageAvailability.PROVIDER_REPORTED
                : UsageAvailability.PARTIAL;

        return new ModelUsage(
                promptTokens,
                null, // cache-read usage is unknown, so uncached input is also unknown
                completionTokens,
                totalTokens,
                null, // cacheReadInputTokens
                null, // cacheWriteInputTokens
                null, // reasoningTokens
                "generic",
                requestedModel != null ? requestedModel : "",
                availability,
                Map.of()
        );
    }
}
