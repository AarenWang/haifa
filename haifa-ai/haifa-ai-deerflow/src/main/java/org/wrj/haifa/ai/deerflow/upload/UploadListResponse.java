package org.wrj.haifa.ai.deerflow.upload;

import java.util.List;

public record UploadListResponse(
    List<UploadResponse> uploads
) {
}
