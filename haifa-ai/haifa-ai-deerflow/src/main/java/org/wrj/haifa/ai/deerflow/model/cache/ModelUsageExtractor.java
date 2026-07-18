package org.wrj.haifa.ai.deerflow.model.cache;

import org.springframework.ai.chat.metadata.ChatResponseMetadata;

public interface ModelUsageExtractor {
    ModelUsage extractUsage(ChatResponseMetadata metadata, String requestedModel);
}
