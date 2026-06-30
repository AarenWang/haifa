package org.wrj.haifa.ai.deerflow.upload;

public record UploadContentResponse(
    String fileId,
    String fileName,
    String content
) {
}
