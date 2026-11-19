package org.wrj.haifa.ai.deerflow.model.cache;

public record PromptCacheContext(
        boolean enabled,
        String provider,
        String routingKey,
        PromptCacheScope scope,
        PromptCacheTtl ttl,
        PromptCacheEligibility eligibility,
        String explicitCachedContentName,
        PromptFingerprint fingerprint) {

    public static PromptCacheContext disabled() {
        return new PromptCacheContext(
                false,
                "",
                "",
                PromptCacheScope.GLOBAL,
                PromptCacheTtl.PROVIDER_DEFAULT,
                PromptCacheEligibility.DISABLED,
                "",
                PromptFingerprint.empty());
    }
}
