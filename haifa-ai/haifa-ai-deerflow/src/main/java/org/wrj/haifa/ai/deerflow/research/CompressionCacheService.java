package org.wrj.haifa.ai.deerflow.research;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.wrj.haifa.ai.deerflow.persistence.entity.CompressionCacheEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.CompressionCacheRepository;
import org.wrj.haifa.ai.deerflow.prompt.PromptCanonicalizer;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Service
public class CompressionCacheService {

    private static final Logger log = LoggerFactory.getLogger(CompressionCacheService.class);

    private final CompressionCacheRepository repository;
    private final DeerFlowProperties properties;
    private final ConcurrentHashMap<String, Object> keyLocks = new ConcurrentHashMap<>();

    @Autowired
    public CompressionCacheService(CompressionCacheRepository repository, DeerFlowProperties properties) {
        this.repository = repository;
        this.properties = properties == null ? new DeerFlowProperties() : properties;
    }

    public CompressionCacheService(CompressionCacheRepository repository) {
        this(repository, new DeerFlowProperties());
    }

    public static String computeCacheKey(
            String schemaVersion,
            String provider,
            String model,
            String promptVersion,
            String contractHash,
            String normalizedUrl,
            String contentHash) {
        String urlHash = PromptCanonicalizer.sha256Hex(normalizedUrl == null ? "" : normalizedUrl);
        String rawKey = (schemaVersion == null ? "v1" : schemaVersion) + ":"
                + (provider == null ? "generic" : provider) + ":"
                + (model == null ? "default" : model) + ":"
                + (promptVersion == null ? "v1" : promptVersion) + ":"
                + (contractHash == null ? "" : contractHash) + ":"
                + urlHash + ":"
                + (contentHash == null ? "" : contentHash);
        return PromptCanonicalizer.sha256Hex(rawKey);
    }

    public String getOrCompute(
            String cacheKey,
            String schemaVersion,
            String provider,
            String model,
            String promptVersion,
            String normalizedUrl,
            String contentHash,
            int sourceChars,
            Duration ttl,
            Supplier<String> computeSupplier) {

        if (cacheKey == null || cacheKey.isBlank()) {
            return computeSupplier.get();
        }

        Optional<String> firstLookup = safeLookup(cacheKey);
        if (firstLookup.isPresent()) {
            log.debug("CompressionCache HIT key={}", PromptCanonicalizer.shortHash(cacheKey));
            return firstLookup.get();
        }

        Object lock = keyLocks.computeIfAbsent(cacheKey, k -> new Object());
        synchronized (lock) {
            try {
                Optional<String> cached = safeLookup(cacheKey);
                if (cached.isPresent()) {
                    return cached.get();
                }

                String computed = computeSupplier.get();
                if (computed != null && !computed.isBlank()) {
                    try {
                        saveCache(cacheKey, schemaVersion, provider, model, promptVersion, normalizedUrl,
                                contentHash, sourceChars, computed, ttl);
                    } catch (Exception e) {
                        log.warn("CompressionCache store failed for key={}: {}. Returning computed value.",
                                PromptCanonicalizer.shortHash(cacheKey), e.getMessage());
                    }
                }
                return computed;
            } finally {
                keyLocks.remove(cacheKey, lock);
            }
        }
    }

    private Optional<String> safeLookup(String cacheKey) {
        try {
            return findValidCache(cacheKey);
        } catch (Exception e) {
            log.warn("CompressionCache lookup failed for key={}: {}. Continuing without cached data.",
                    PromptCanonicalizer.shortHash(cacheKey), e.getMessage());
            return Optional.empty();
        }
    }

    @Transactional
    public Optional<String> findValidCache(String cacheKey) {
        return repository.findByCacheKeyAndExpiresAtAfter(cacheKey, Instant.now())
                .map(entity -> {
                    entity.setHitCount(entity.getHitCount() + 1);
                    entity.setLastAccessedAt(Instant.now());
                    repository.save(entity);
                    return entity.getCompressedBody();
                });
    }

    @Transactional
    public void saveCache(
            String cacheKey,
            String schemaVersion,
            String provider,
            String model,
            String promptVersion,
            String normalizedUrl,
            String contentHash,
            int sourceChars,
            String compressedBody,
            Duration ttl) {

        Instant now = Instant.now();
        Duration effectiveTtl = (ttl == null || ttl.isNegative() || ttl.isZero()) ? Duration.ofDays(7) : ttl;

        CompressionCacheEntity entity = new CompressionCacheEntity();
        entity.setCacheKey(cacheKey);
        entity.setSchemaVersion(schemaVersion != null ? schemaVersion : "v1");
        entity.setProvider(provider != null ? provider : "generic");
        entity.setModel(model != null ? model : "default");
        entity.setPromptVersion(promptVersion != null ? promptVersion : "evidence-compressor-v1");
        entity.setContentHash(contentHash != null ? contentHash : "");
        entity.setNormalizedUrlHash(PromptCanonicalizer.sha256Hex(normalizedUrl != null ? normalizedUrl : ""));
        entity.setCompressedBody(compressedBody);
        entity.setSourceChars(sourceChars);
        entity.setCompressedChars(compressedBody.length());
        entity.setCreatedAt(now);
        entity.setLastAccessedAt(now);
        entity.setExpiresAt(now.plus(effectiveTtl));
        entity.setHitCount(0);
        entity.setStatus("ACTIVE");

        repository.save(entity);
        enforceCapacity();
    }

    @Transactional
    public int cleanupExpired() {
        return repository.deleteExpiredBefore(Instant.now());
    }

    @org.springframework.scheduling.annotation.Scheduled(
            fixedDelayString = "${haifa.ai.deerflow.prompt-cache.compression.cleanup-interval:PT6H}",
            initialDelayString = "${haifa.ai.deerflow.prompt-cache.compression.cleanup-interval:PT6H}")
    public void scheduledCleanup() {
        if (!properties.getPromptCache().isEnabled()
                || !properties.getPromptCache().getCompression().isEnabled()) {
            return;
        }
        try {
            int deleted = cleanupExpired();
            if (deleted > 0) {
                log.info("CompressionCache cleanup removed {} expired entries", deleted);
            }
            enforceCapacity();
        } catch (Exception e) {
            log.warn("CompressionCache cleanup failed: {}", e.getMessage());
        }
    }

    private void enforceCapacity() {
        int maxEntries = properties.getPromptCache().getCompression().getMaxEntries();
        if (maxEntries <= 0) {
            return;
        }
        long excess = repository.count() - maxEntries;
        if (excess <= 0) {
            return;
        }
        int deleteCount = (int) Math.min(excess, Integer.MAX_VALUE);
        java.util.List<CompressionCacheEntity> oldest = repository
                .findAllByOrderByLastAccessedAtAsc(org.springframework.data.domain.PageRequest.of(0, deleteCount));
        if (!oldest.isEmpty()) {
            repository.deleteAllInBatch(oldest);
        }
    }
}
