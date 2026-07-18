package org.wrj.haifa.ai.deerflow.model.cache;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.google.genai.metadata.GoogleGenAiUsage;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

@Component
@Order(0)
public class GoogleGenAiPromptCacheProviderAdapter implements PromptCacheProviderAdapter {

    private final DeerFlowProperties properties;

    public GoogleGenAiPromptCacheProviderAdapter() {
        this(new DeerFlowProperties());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public GoogleGenAiPromptCacheProviderAdapter(DeerFlowProperties properties) {
        this.properties = properties == null ? new DeerFlowProperties() : properties;
    }

    @Override
    public String providerId() {
        return "google-genai";
    }

    @Override
    public boolean supports(ModelPrompt prompt) {
        if (prompt == null) {
            return false;
        }
        String model = prompt.modelName() == null ? "" : prompt.modelName().toLowerCase();
        return model.contains("gemini") || model.contains("google");
    }

    @Override
    public ChatOptions buildOptions(ModelPrompt prompt, List<ToolCallback> callbacks) {
        GoogleGenAiChatOptions.Builder builder = GoogleGenAiChatOptions.builder();
        if (StringUtils.hasText(prompt.modelName())) {
            builder.model(prompt.modelName());
        }
        if (callbacks != null && !callbacks.isEmpty()) {
            builder.toolCallbacks(callbacks);
        }
        builder.internalToolExecutionEnabled(false);
        DeerFlowProperties.PromptCache.GooglePromptCache config = properties.getPromptCache().getGoogle();
        builder.includeExtendedUsageMetadata(config.isIncludeExtendedUsageMetadata());

        if (properties.getPromptCache().isEnabled() && config.isEnabled() && config.isAutoCacheEnabled()) {
            builder.autoCacheThreshold(Math.max(1, config.getAutoCacheThreshold()));
            builder.autoCacheTtl(parsePositiveDuration(config.getAutoCacheTtl(), Duration.ofMinutes(30)));
        }

        if (properties.getPromptCache().isEnabled() && config.isEnabled()
                && prompt.cacheContext() != null && prompt.cacheContext().enabled()
                && StringUtils.hasText(prompt.cacheContext().explicitCachedContentName())) {
            builder.cachedContentName(prompt.cacheContext().explicitCachedContentName());
            builder.useCachedContent(true);
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
        Map<String, Long> details = new HashMap<>();

        Object nativeUsage = usage.getNativeUsage();
        GoogleGenAiUsage googleUsage = usage instanceof GoogleGenAiUsage direct ? direct
                : nativeUsage instanceof GoogleGenAiUsage nested ? nested : null;
        if (googleUsage != null) {
            cacheReadInputTokens = longValue(googleUsage.getCachedContentTokenCount());
            putIfPresent(details, "thoughtsTokenCount", googleUsage.getThoughtsTokenCount());
            putIfPresent(details, "toolUsePromptTokenCount", googleUsage.getToolUsePromptTokenCount());
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
                details.get("thoughtsTokenCount"), // reasoningTokens from thoughts
                "google-genai",
                requestedModel != null ? requestedModel : "",
                availability,
                details
        );
    }

    private static Duration parsePositiveDuration(String configured, Duration fallback) {
        try {
            Duration parsed = Duration.parse(configured);
            return parsed.isNegative() || parsed.isZero() ? fallback : parsed;
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static Long longValue(Number value) {
        return value == null ? null : value.longValue();
    }

    private static void putIfPresent(Map<String, Long> details, String key, Number value) {
        if (value != null) {
            details.put(key, value.longValue());
        }
    }
}
