package org.wrj.haifa.ai.deerflow.model.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class GoogleGenAiExplicitContextCacheRegistry implements ExplicitContextCacheRegistry {

    private final ConcurrentHashMap<String, CacheEntry> entries = new ConcurrentHashMap<>();

    @Override
    public Optional<CacheEntry> getValidEntry(String cacheKey) {
        if (cacheKey == null || cacheKey.isBlank()) {
            return Optional.empty();
        }
        CacheEntry entry = entries.get(cacheKey);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now()) || "EXPIRED".equalsIgnoreCase(entry.status()) || "FAILED".equalsIgnoreCase(entry.status())) {
            return Optional.empty();
        }
        // Update last used
        CacheEntry updated = new CacheEntry(
                entry.cacheKey(), entry.provider(), entry.model(), entry.scope(), entry.contentKind(),
                entry.contentHash(), entry.promptVersion(), entry.systemInstructionHash(), entry.toolDefinitionsHash(),
                entry.remoteResourceName(), entry.tokenCount(), entry.createdAt(), entry.expiresAt(),
                Instant.now(), entry.status(), entry.failureCode()
        );
        entries.put(cacheKey, updated);
        return Optional.of(updated);
    }

    @Override
    public CacheEntry registerEntry(
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
            Duration ttl) {

        String keyInput = (provider == null ? "google-genai" : provider) + ":"
                + (model == null ? "default" : model) + ":"
                + (scope == null ? PromptCacheScope.GLOBAL : scope) + ":"
                + (contentKind == null ? "doc" : contentKind) + ":"
                + (contentHash == null ? "" : contentHash) + ":"
                + (promptVersion == null ? "v1" : promptVersion) + ":"
                + (systemInstructionHash == null ? "" : systemInstructionHash) + ":"
                + (toolDefinitionsHash == null ? "" : toolDefinitionsHash);

        String cacheKey = org.wrj.haifa.ai.deerflow.prompt.PromptCanonicalizer.sha256Hex(keyInput);
        Instant now = Instant.now();
        Duration effectiveTtl = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofMinutes(30) : ttl;

        CacheEntry entry = new CacheEntry(
                cacheKey,
                provider != null ? provider : "google-genai",
                model != null ? model : "default",
                scope != null ? scope : PromptCacheScope.GLOBAL,
                contentKind != null ? contentKind : "doc",
                contentHash != null ? contentHash : "",
                promptVersion != null ? promptVersion : "v1",
                systemInstructionHash != null ? systemInstructionHash : "",
                toolDefinitionsHash != null ? toolDefinitionsHash : "",
                remoteResourceName != null ? remoteResourceName : "",
                tokenCount,
                now,
                now.plus(effectiveTtl),
                now,
                "READY",
                null
        );

        entries.put(cacheKey, entry);
        return entry;
    }

    @Override
    public void markExpiredOrInvalid(String cacheKey, String failureCode) {
        if (cacheKey == null) {
            return;
        }
        CacheEntry entry = entries.get(cacheKey);
        if (entry != null) {
            CacheEntry updated = new CacheEntry(
                    entry.cacheKey(), entry.provider(), entry.model(), entry.scope(), entry.contentKind(),
                    entry.contentHash(), entry.promptVersion(), entry.systemInstructionHash(), entry.toolDefinitionsHash(),
                    entry.remoteResourceName(), entry.tokenCount(), entry.createdAt(), entry.expiresAt(),
                    entry.lastUsedAt(), "EXPIRED", failureCode
            );
            entries.put(cacheKey, updated);
        }
    }

    @Override
    public void evict(String cacheKey) {
        if (cacheKey != null) {
            entries.remove(cacheKey);
        }
    }
}
