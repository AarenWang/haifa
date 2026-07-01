package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.List;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.skill.Skill;

public record ToolRequest(String userMessage, Path workspaceRoot, List<String> uploadedFileIds, String threadId,
        String runId, RunMode mode, List<Skill> activeSkills) {

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
        this.userMessage = userMessage;
        this.workspaceRoot = workspaceRoot;
        this.uploadedFileIds = uploadedFileIds == null ? List.of() : uploadedFileIds;
        this.threadId = threadId;
        this.runId = runId;
        this.mode = mode == null ? RunMode.RESEARCH : mode;
        this.activeSkills = activeSkills == null ? List.of() : List.copyOf(activeSkills);
    }
}
