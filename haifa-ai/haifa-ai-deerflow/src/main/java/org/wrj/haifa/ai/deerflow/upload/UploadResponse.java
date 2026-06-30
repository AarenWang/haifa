package org.wrj.haifa.ai.deerflow.upload;

import java.time.Instant;

public record UploadResponse(
    String fileId,
    String fileName,
    String mimeType,
    long fileSize,
    String extension,
    String threadId,
    String createdAt,
    String status,
    String contentPreview
) {
}
