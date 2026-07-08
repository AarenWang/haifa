package org.wrj.haifa.ai.deerflow.web;

public record ArtifactResponse(
        String artifactId,
        String runId,
        String threadId,
        String filename,
        String mimeType,
        long size,
        String createdAt,
        String preview,
        boolean previewTruncated,
        boolean renderable,
        boolean sourceViewable,
        String downloadUrl,
        String rawUrl
) {
}