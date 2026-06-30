package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;

public record ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId) {

    public ToolRequest(String userMessage, Path workspaceRoot) {
        this(userMessage, workspaceRoot, List.of(), null);
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds) {
        this(userMessage, workspaceRoot, uploadedFileIds, null);
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId) {
        this.userMessage = userMessage;
        this.workspaceRoot = workspaceRoot;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        this.threadId = threadId;
    }
}
