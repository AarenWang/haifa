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
        int schemaVersion,
        String graphDefinitionVersion,
        Map<String, Object> stateSummary,
        Map<String, Object> fullState,
        Instant createdAt
) {
    /** Compatibility constructor for records produced before phase 62. */
    public AgentGraphCheckpointRecord(String recordId, String checkpointId, String runId, String threadId,
            String graphName, String nodeId, String nextNodeId, Map<String, Object> stateSummary,
            Map<String, Object> fullState, Instant createdAt) {
        this(recordId, checkpointId, runId, threadId, graphName, nodeId, nextNodeId, 1, "legacy",
                stateSummary, fullState, createdAt);
    }
}
