package org.wrj.haifa.ai.deerflow.model.cache;

import java.util.List;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.util.StringUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

@Component
@Order(0)
public class OpenAiPromptCacheProviderAdapter implements PromptCacheProviderAdapter {

    private final DeerFlowProperties properties;

    public OpenAiPromptCacheProviderAdapter() {
        this(new DeerFlowProperties());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public OpenAiPromptCacheProviderAdapter(DeerFlowProperties properties) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
    }

    @Override
    public String providerId() {
        return "openai";
    }

    @Override
    public boolean supports(ModelPrompt prompt) {
        if (prompt == null) {
            return false;
        }
        String model = prompt.modelName() == null ? "" : prompt.modelName().toLowerCase();
        return model.contains("gpt") || model.contains("openai");
    }

    @Override
    public ChatOptions buildOptions(ModelPrompt prompt, List<ToolCallback> callbacks) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (StringUtils.hasText(prompt.modelName())) {
            builder.model(prompt.modelName());
        }
        if (callbacks != null && !callbacks.isEmpty()) {
            builder.toolCallbacks(callbacks);
        }
        builder.internalToolExecutionEnabled(false);
        builder.streamUsage(true);

        boolean cacheKeyEnabled = properties.getPromptCache().isEnabled()
                && properties.getPromptCache().getOpenai().isEnabled()
                && properties.getPromptCache().getOpenai().isPromptCacheKeyEnabled()
                && prompt.cacheContext() != null;
        if (cacheKeyEnabled && StringUtils.hasText(prompt.cacheContext().routingKey())) {
            builder.promptCacheKey(prompt.cacheContext().routingKey());
        } else if (cacheKeyEnabled && prompt.cacheContext().fingerprint() != null && StringUtils.hasText(prompt.cacheContext().fingerprint().cacheablePrefixHash())) {
            String shortHash = prompt.cacheContext().fingerprint().cacheablePrefixHash();
            if (shortHash.length() > 16) {
                shortHash = shortHash.substring(0, 16);
            }
            builder.promptCacheKey("df:v1:openai:" + shortHash);
        }

        return builder.build();
    }

    @Override
    public ModelUsage extractUsage(ChatResponseMetadata metadata, String requestedModel) {
        if (metadata == null || metadata.getUsage() == null) {
            return ModelUsage.empty();
        }
        Usage usage = metadata.getUsage();
        Long promptTokens = usage.getPromptTokens() != null ? usage.getPromptTokens().longValue() : null;
        Long completionTokens = usage.getCompletionTokens() != null ? usage.getCompletionTokens().longValue() : null;
        Long totalTokens = usage.getTotalTokens() != null ? usage.getTotalTokens().longValue() : null;

        Long cacheReadInputTokens = null;

        Object nativeUsage = usage.getNativeUsage();
        if (nativeUsage instanceof OpenAiApi.Usage openAiUsage) {
            if (openAiUsage.promptTokensDetails() != null && openAiUsage.promptTokensDetails().cachedTokens() != null) {
                cacheReadInputTokens = openAiUsage.promptTokensDetails().cachedTokens().longValue();
            }
        }

        Long uncachedInputTokens = promptTokens != null && cacheReadInputTokens != null
                ? Math.max(0L, promptTokens - cacheReadInputTokens)
                : null;

        UsageAvailability availability = (promptTokens != null || completionTokens != null)
                ? UsageAvailability.PROVIDER_REPORTED
                : UsageAvailability.UNAVAILABLE;

        return new ModelUsage(
                promptTokens,
                uncachedInputTokens,
                completionTokens,
                totalTokens,
                cacheReadInputTokens,
                null, // cacheWriteInputTokens
                null, // reasoningTokens
                "openai",
                requestedModel != null ? requestedModel : "",
                availability,
                java.util.Map.of()
        );
    }
}
