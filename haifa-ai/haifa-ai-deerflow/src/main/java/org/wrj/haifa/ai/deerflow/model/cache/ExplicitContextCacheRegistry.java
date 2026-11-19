package org.wrj.haifa.ai.deerflow.model.cache;

import java.time.Duration;
import java.util.Optional;

public interface ExplicitContextCacheRegistry {

    record CacheEntry(
            String cacheKey,
            String provider,
            String model,
            PromptCacheScope scope,
            String contentKind,
            String contentHash,
            String promptVersion,
            String systemInstructionHash,
            String toolDefinitionsHash,
            String remoteResourceName,
            int tokenCount,
            java.time.Instant createdAt,
            java.time.Instant expiresAt,
            java.time.Instant lastUsedAt,
            String status,
            String failureCode
    ) {}

    Optional<CacheEntry> getValidEntry(String cacheKey);

    CacheEntry registerEntry(
            String provider,
            String model,
            PromptCacheScope scope,
            String contentKind,
            String contentHash,
            String promptVersion,
            String systemInstructionHash,
            String toolDefinitionsHash,
            String remoteResourceName,
            int tokenCount,
            Duration ttl
    );

    void markExpiredOrInvalid(String cacheKey, String failureCode);

    void evict(String cacheKey);
}
