package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;

public record ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
        String runId) {

    public ToolRequest(String userMessage, Path workspaceRoot) {
        this(userMessage, workspaceRoot, List.of(), null, null);
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds) {
        this(userMessage, workspaceRoot, uploadedFileIds, null, null);
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId) {
        this(userMessage, workspaceRoot, uploadedFileIds, threadId, null);
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
            String runId) {
        this.userMessage = userMessage;
        this.workspaceRoot = workspaceRoot;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        this.threadId = threadId;
        this.runId = runId;
    }
}
