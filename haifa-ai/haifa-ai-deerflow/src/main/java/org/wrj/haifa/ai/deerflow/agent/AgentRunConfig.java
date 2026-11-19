package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Path;
import java.util.Map;

public record AgentRunConfig(
        String threadId,
        String runId,
        String modelName,
        boolean thinkingEnabled,
        boolean planMode,
        int maxIterations,
        Path workspaceRoot,
        RunMode mode,
        ResearchOptions researchOptions,
        Map<String, Object> metadata
) {

    public AgentRunConfig {
        mode = mode == null ? RunMode.CHAT : mode;
        researchOptions = researchOptions == null ? ResearchOptions.defaults() : researchOptions;
    }

    public AgentRunConfig(String threadId, String runId, String modelName, boolean thinkingEnabled, boolean planMode,
            int maxIterations, Path workspaceRoot, Map<String, Object> metadata) {
        this(threadId, runId, modelName, thinkingEnabled, planMode, maxIterations, workspaceRoot, RunMode.CHAT,
                ResearchOptions.defaults(), metadata);
    }
}
