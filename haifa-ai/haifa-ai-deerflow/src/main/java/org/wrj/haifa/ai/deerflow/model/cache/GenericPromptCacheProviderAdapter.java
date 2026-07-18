package org.wrj.haifa.ai.deerflow.model.cache;

import java.util.List;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

public class GenericPromptCacheProviderAdapter implements PromptCacheProviderAdapter {

    private final GenericModelUsageExtractor usageExtractor = new GenericModelUsageExtractor();

    @Override
    public String providerId() {
        return "generic";
    }

    @Override
    public boolean supports(ModelPrompt prompt) {
        return true;
    }

    @Override
    public ChatOptions buildOptions(ModelPrompt prompt, List<ToolCallback> callbacks) {
        if (callbacks == null || callbacks.isEmpty()) {
            return StringUtils.hasText(prompt.modelName())
                    ? ChatOptions.builder().model(prompt.modelName()).build()
                    : null;
        }
        ToolCallingChatOptions.Builder optionsBuilder = ToolCallingChatOptions.builder()
                .toolCallbacks(callbacks)
                .internalToolExecutionEnabled(false);
        if (StringUtils.hasText(prompt.modelName())) {
            optionsBuilder.model(prompt.modelName());
        }
        return optionsBuilder.build();
    }

    @Override
    public ModelUsage extractUsage(ChatResponseMetadata metadata, String requestedModel) {
        return usageExtractor.extractUsage(metadata, requestedModel);
    }
}
