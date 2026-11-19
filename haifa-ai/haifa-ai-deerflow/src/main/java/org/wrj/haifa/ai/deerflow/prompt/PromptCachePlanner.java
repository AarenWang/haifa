package org.wrj.haifa.ai.deerflow.prompt;

import java.util.List;
import java.util.Locale;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelToolDefinition;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlock;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockPlacement;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlockType;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheContext;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheEligibility;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheScope;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheTtl;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheability;
import org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint;
import org.wrj.haifa.ai.deerflow.model.cache.PromptStability;

/** Finalizes cache metadata from the exact prompt sent to the provider. */
public final class PromptCachePlanner {

    private static final int OPENAI_MIN_CACHEABLE_TOKENS = 1_024;

    private PromptCachePlanner() {
    }

    public static ModelPrompt enrich(ModelPrompt prompt, DeerFlowProperties properties) {
        if (prompt == null) {
            throw new IllegalArgumentException("prompt must not be null");
        }
        List<ModelToolDefinition> stableTools = prompt.toolDefinitions().stream()
                .sorted(java.util.Comparator.comparing(tool -> tool.name() == null ? "" : tool.name()))
                .toList();
        ModelPrompt stablePrompt = prompt.withToolDefinitions(stableTools);
        DeerFlowProperties effectiveProperties = properties == null ? new DeerFlowProperties() : properties;
        DeerFlowProperties.PromptCache config = effectiveProperties.getPromptCache();
        if (config == null) {
            config = new DeerFlowProperties.PromptCache();
        }
        String schemaVersion = textOrDefault(config.getCanonicalizationVersion(), "v1");

        PromptBlock staticSystem = new PromptBlock(
                PromptBlockType.STATIC_SYSTEM,
                PromptBlockPlacement.SYSTEM_PREFIX,
                PromptStability.GLOBAL,
                PromptCacheability.CACHEABLE,
                schemaVersion,
                stablePrompt.systemPrompt());
        List<PromptBlock> fingerprintBlocks = List.of(staticSystem);

        List<ModelMessage> messages = stablePrompt.messages();
        String conversationPrefix = "";
        String dynamicTail = stablePrompt.userPrompt();
        if (!messages.isEmpty()) {
            int lastIndex = messages.size() - 1;
            conversationPrefix = ModelPrompt.renderMessages(messages.subList(0, lastIndex));
            dynamicTail = messages.get(lastIndex).content();
        }

        PromptFingerprint fingerprint = new PromptFingerprintService().computeFingerprint(
                schemaVersion,
                fingerprintBlocks,
                stablePrompt.toolDefinitions(),
                conversationPrefix,
                dynamicTail);

        String provider = providerFor(stablePrompt.modelName());
        boolean providerEnabled = switch (provider) {
            case "openai" -> config.getOpenai().isEnabled();
            case "google-genai" -> config.getGoogle().isEnabled();
            default -> false;
        };
        boolean enabled = config.isEnabled() && providerEnabled;
        PromptCacheEligibility eligibility = eligibility(provider, enabled, fingerprint, config);
        String routingKey = enabled ? routingKey(schemaVersion, provider, stablePrompt.modelName(), fingerprint,
                config.getRoutingShards()) : "";

        PromptCacheContext context = new PromptCacheContext(
                enabled,
                provider,
                routingKey,
                PromptCacheScope.CAPABILITY_SET,
                PromptCacheTtl.PROVIDER_DEFAULT,
                eligibility,
                stablePrompt.cacheContext().explicitCachedContentName(),
                fingerprint);
        return stablePrompt.withPromptBlocks(fingerprintBlocks).withCacheContext(context);
    }

    private static PromptCacheEligibility eligibility(String provider, boolean enabled,
            PromptFingerprint fingerprint, DeerFlowProperties.PromptCache config) {
        if (!config.isEnabled()) {
            return PromptCacheEligibility.DISABLED;
        }
        if (!enabled) {
            return provider.isBlank() ? PromptCacheEligibility.UNKNOWN : PromptCacheEligibility.UNSUPPORTED;
        }
        int minimum = switch (provider) {
            case "openai" -> OPENAI_MIN_CACHEABLE_TOKENS;
            case "google-genai" -> Math.max(1, config.getGoogle().getAutoCacheThreshold());
            default -> Integer.MAX_VALUE;
        };
        return fingerprint.estimatedCacheablePrefixTokens() >= minimum
                ? PromptCacheEligibility.ELIGIBLE
                : PromptCacheEligibility.BELOW_MINIMUM;
    }

    private static String providerFor(String modelName) {
        String model = modelName == null ? "" : modelName.toLowerCase(Locale.ROOT);
        if (model.contains("gemini") || model.contains("google")) {
            return "google-genai";
        }
        if (model.contains("gpt") || model.contains("openai")
                || model.matches(".*(?:^|[-_])o[134](?:[-_].*)?$")) {
            return "openai";
        }
        return "";
    }

    private static String routingKey(String version, String provider, String model,
            PromptFingerprint fingerprint, int configuredShards) {
        int shards = Math.max(1, configuredShards);
        String hash = fingerprint.cacheablePrefixHash();
        int shard = Math.floorMod(hash.hashCode(), shards);
        String modelHash = PromptCanonicalizer.shortHash(
                PromptCanonicalizer.sha256Hex(textOrDefault(model, "default")));
        return "df:" + version + ":" + provider + ":m" + modelHash
                + ":s" + shard + ":" + PromptCanonicalizer.shortHash(hash);
    }

    private static String textOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
