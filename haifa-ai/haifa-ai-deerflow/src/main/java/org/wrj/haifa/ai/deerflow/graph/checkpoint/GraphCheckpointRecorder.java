package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.MemorySaver;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class GraphCheckpointRecorder {

    private final AgentGraphCheckpointStore store;

    public GraphCheckpointRecorder(AgentGraphCheckpointStore store) {
        this.store = store;
    }

    public List<AgentGraphCheckpointRecord> record(String graphName, AgentRunConfig runConfig,
            RunnableConfig runnableConfig, MemorySaver saver) {
        if (runConfig == null || runnableConfig == null || saver == null) {
            return List.of();
        }
        List<AgentGraphCheckpointRecord> records = saver.list(runnableConfig).stream()
                .map(checkpoint -> toRecord(graphName, runConfig, checkpoint))
                .toList();
        store.saveAll(records);
        return records;
    }

    private AgentGraphCheckpointRecord toRecord(String graphName, AgentRunConfig runConfig, Checkpoint checkpoint) {
        return new AgentGraphCheckpointRecord(
                UUID.randomUUID().toString(),
                safe(checkpoint.getId()),
                runConfig.runId(),
                runConfig.threadId(),
                safe(graphName),
                safe(checkpoint.getNodeId()),
                safe(checkpoint.getNextNodeId()),
                summarize(checkpoint.getState()),
                checkpoint.getState() == null ? Map.of() : checkpoint.getState(),
                Instant.now()
        );
    }

    private Map<String, Object> summarize(Map<String, Object> state) {
        AgentGraphStateView view = AgentGraphStateView.of(state);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", view.runId());
        summary.put("threadId", view.threadId());
        summary.put("mode", view.mode().name());
        summary.put("modelName", view.modelName());
        summary.put("messageWindowSize", view.messageWindow().size());
        summary.put("modelStepCount", view.modelSteps().size());
        summary.put("toolCallCount", view.toolCalls().size());
        summary.put("toolResultCount", view.toolResults().size());
        summary.put("artifactCount", view.artifacts().size());
        summary.put("hasFinalAnswer", !view.finalAnswer().isBlank());
        return Map.copyOf(summary);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
