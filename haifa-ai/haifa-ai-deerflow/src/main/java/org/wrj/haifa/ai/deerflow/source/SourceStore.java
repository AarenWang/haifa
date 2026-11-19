package org.wrj.haifa.ai.deerflow.source;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SourceStore {

    private final SourceRepository repository;

    public SourceStore(SourceRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Source discover(String runId, String threadId, String url, String title, String domain) {
        Optional<Source> existing = repository.findByUrlAndThreadIdAndRunId(url, threadId, runId);
        if (existing.isPresent()) {
            return existing.get();
        }
        Source source = new Source();
        source.setSourceId(UUID.randomUUID().toString());
        source.setRunId(runId);
        source.setThreadId(threadId);
        source.setUrl(url);
        source.setTitle(title);
        source.setDomain(domain);
        source.setLifecycleStatus("discovered");
        source.setSourceType("unknown");
        source.setQualityTier("unknown");
        source.setLastCheckedAt(Instant.now());
        return repository.save(source);
    }

    @Transactional
    public Source updateFetched(String sourceId, String canonicalUrl, String title, String sourceType, String qualityTier, String contentHash, String metadataJson) {
        return repository.findById(sourceId).map(source -> {
            source.setCanonicalUrl(canonicalUrl);
            if (title != null && !title.isBlank()) {
                source.setTitle(title);
            }
            source.setSourceType(sourceType != null ? sourceType : "unknown");
            source.setQualityTier(qualityTier != null ? qualityTier : "unknown");
            source.setLifecycleStatus("fetched");
            source.setContentHash(contentHash);
            source.setFetchedAt(Instant.now());
            source.setLastCheckedAt(Instant.now());
            source.setMetadataJson(metadataJson);
            return repository.save(source);
        }).orElse(null);
    }

    @Transactional
    public Source updateStatus(String sourceId, String status, String discardReason) {
        return repository.findById(sourceId).map(source -> {
            source.setLifecycleStatus(status);
            source.setDiscardReason(discardReason);
            source.setLastCheckedAt(Instant.now());
            return repository.save(source);
        }).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<Source> findByRunId(String runId) {
        return repository.findByRunId(runId);
    }

    @Transactional(readOnly = true)
    public List<Source> findByThreadId(String threadId) {
        return repository.findByThreadId(threadId);
    }

    @Transactional(readOnly = true)
    public Optional<Source> findByUrlAndThreadId(String url, String threadId) {
        return repository.findByUrlAndThreadId(url, threadId);
    }
    @Transactional(readOnly = true)
    public Optional<Source> findByUrlAndThreadIdAndRunId(String url, String threadId, String runId) {
        return repository.findByUrlAndThreadIdAndRunId(url, threadId, runId);
    }
}