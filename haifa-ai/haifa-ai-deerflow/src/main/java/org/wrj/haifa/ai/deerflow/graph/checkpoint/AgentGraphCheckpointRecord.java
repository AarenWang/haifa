package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import java.time.Instant;
import java.util.Map;

public record AgentGraphCheckpointRecord(
        String recordId,
        String checkpointId,
        String runId,
        String threadId,
        String graphName,
        String nodeId,
        String nextNodeId,
        Map<String, Object> stateSummary,
        Map<String, Object> fullState,
        Instant createdAt
) {
}
