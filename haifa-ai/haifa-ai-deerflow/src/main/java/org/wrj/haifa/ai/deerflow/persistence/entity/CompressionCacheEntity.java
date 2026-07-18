package org.wrj.haifa.ai.deerflow.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "deerflow_compression_cache", indexes = {
        @Index(name = "idx_comp_cache_content_hash", columnList = "content_hash"),
        @Index(name = "idx_comp_cache_expires_at", columnList = "expires_at")
})
public class CompressionCacheEntity {

    @Id
    @Column(name = "cache_key", length = 64, nullable = false)
    private String cacheKey;

    @Column(name = "schema_version", length = 32, nullable = false)
    private String schemaVersion;

    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "model", length = 64)
    private String model;

    @Column(name = "prompt_version", length = 64)
    private String promptVersion;

    @Column(name = "content_hash", length = 64, nullable = false)
    private String contentHash;

    @Column(name = "normalized_url_hash", length = 64)
    private String normalizedUrlHash;

    @Lob
    @Column(name = "compressed_body", nullable = false)
    private String compressedBody;

    @Column(name = "source_chars", nullable = false)
    private int sourceChars;

    @Column(name = "compressed_chars", nullable = false)
    private int compressedChars;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "hit_count", nullable = false)
    private int hitCount;

    @Column(name = "status", length = 32, nullable = false)
    private String status;

    public CompressionCacheEntity() {
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(String schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getNormalizedUrlHash() {
        return normalizedUrlHash;
    }

    public void setNormalizedUrlHash(String normalizedUrlHash) {
        this.normalizedUrlHash = normalizedUrlHash;
    }

    public String getCompressedBody() {
        return compressedBody;
    }

    public void setCompressedBody(String compressedBody) {
        this.compressedBody = compressedBody;
    }

    public int getSourceChars() {
        return sourceChars;
    }

    public void setSourceChars(int sourceChars) {
        this.sourceChars = sourceChars;
    }

    public int getCompressedChars() {
        return compressedChars;
    }

    public void setCompressedChars(int compressedChars) {
        this.compressedChars = compressedChars;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public void setLastAccessedAt(Instant lastAccessedAt) {
        this.lastAccessedAt = lastAccessedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
