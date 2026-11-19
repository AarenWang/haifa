package org.wrj.haifa.ai.deerflow.model.cache;

import java.util.List;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

public interface PromptCacheProviderAdapter {
    String providerId();
    boolean supports(ModelPrompt prompt);
    ChatOptions buildOptions(ModelPrompt prompt, List<ToolCallback> callbacks);
    ModelUsage extractUsage(ChatResponseMetadata metadata, String requestedModel);
}
