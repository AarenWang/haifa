package org.wrj.haifa.ai.deerflow.artifact;

import java.time.Instant;

public record ArtifactRecord(
        String artifactId,
        String runId,
        String threadId,
        String path,
        String filename,
        String mimeType,
        long size,
        Instant createdAt
) {

    public ArtifactRecord {
        path = path == null ? "" : path;
        filename = filename == null ? "" : filename;
        mimeType = mimeType == null || mimeType.isBlank() ? "application/octet-stream" : mimeType;
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }
}
