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
        Map<String, Object> metadata
) {
}
