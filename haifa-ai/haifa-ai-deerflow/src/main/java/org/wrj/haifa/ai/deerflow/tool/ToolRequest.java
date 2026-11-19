package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.skill.Skill;
import org.wrj.haifa.ai.deerflow.run.RunCancellationToken;

public record ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
        String runId, RunMode mode, List<Skill> activeSkills, String modelName,
        RunCancellationToken cancellationToken) {

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
            String runId, RunMode mode, List<Skill> activeSkills, String modelName) {
        this(userMessage, workspaceRoot, uploadedFileIds, threadId, runId, mode, activeSkills, modelName, null);
    }

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
        this(userMessage, workspaceRoot, uploadedFileIds, threadId, runId, RunMode.RESEARCH, List.of());
    }

    public ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
            String runId, RunMode mode, List<Skill> activeSkills) {
        this(userMessage, workspaceRoot, uploadedFileIds, threadId, runId, mode, activeSkills, null, null);
    }

    public ToolRequest {
        uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        mode = mode == null ? RunMode.RESEARCH : mode;
        activeSkills = activeSkills == null ? List.of() : List.copyOf(activeSkills);
        modelName = modelName == null ? "" : modelName;
        cancellationToken = cancellationToken == null ? new RunCancellationToken(runId) : cancellationToken;
    }
}
