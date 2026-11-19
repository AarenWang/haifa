package org.wrj.haifa.ai.deerflow.graph;

import java.util.List;
import java.util.Map;

public record AgentGraphShadowResult(
        String runId,
        String threadId,
        List<String> visitedNodes,
        Map<String, Object> finalState,
        long durationMs
) {
}
