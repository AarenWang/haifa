package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class SQLiteCheckpointSaver implements BaseCheckpointSaver {

    private final AgentGraphCheckpointStore store;

    public SQLiteCheckpointSaver(AgentGraphCheckpointStore store) {
        this.store = store;
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        if (config == null || config.threadId().isEmpty()) {
            return Optional.empty();
        }
        String threadId = config.threadId().orElse("");
        List<AgentGraphCheckpointRecord> records = store.findByThreadId(threadId);
        if (records.isEmpty()) {
            return Optional.empty();
        }
        AgentGraphCheckpointRecord record = records.get(records.size() - 1);
        return Optional.of(toCheckpoint(record));
    }

    @Override
    public RunnableConfig put(RunnableConfig config, Checkpoint checkpoint) {
        if (config == null || checkpoint == null) {
            return config;
        }
        String threadId = config.threadId().orElse("");
        AgentGraphStateView view = AgentGraphStateView.of(checkpoint.getState());
        String runId = view.runId();

        AgentGraphCheckpointRecord record = new AgentGraphCheckpointRecord(
                UUID.randomUUID().toString(),
                checkpoint.getId(),
                runId == null ? "" : runId,
                threadId,
                graphName(view.mode()),
                checkpoint.getNodeId(),
                normalizeNextNodeId(checkpoint.getNextNodeId()),
                summarize(checkpoint.getState()),
                checkpoint.getState(),
                Instant.now()
        );
        store.saveAll(List.of(record));
        return config;
    }

    @Override
    public Collection<Checkpoint> list(RunnableConfig config) {
        if (config == null || config.threadId().isEmpty()) {
            return List.of();
        }
        String threadId = config.threadId().orElse("");
        List<AgentGraphCheckpointRecord> records = store.findByThreadId(threadId);
        return records.stream().map(this::toCheckpoint).toList();
    }

    @Override
    public BaseCheckpointSaver.Tag release(RunnableConfig config) {
        return new BaseCheckpointSaver.Tag("sqlite-tag", List.of());
    }

    private Checkpoint toCheckpoint(AgentGraphCheckpointRecord record) {
        return Checkpoint.builder()
                .id(record.checkpointId())
                .nodeId(record.nodeId())
                .nextNodeId(record.nextNodeId())
                .state(record.fullState() == null ? Map.of() : record.fullState())
                .build();
    }

    private static String normalizeNextNodeId(String nextNodeId) {
        if (StateGraph.END.equals(nextNodeId)) {
            return "";
        }
        return nextNodeId;
    }

    private static String graphName(RunMode mode) {
        return mode == RunMode.RESEARCH ? "haifa-active-research" : "haifa-active-chat";
    }

    private static Map<String, Object> summarize(Map<String, Object> state) {
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
}
