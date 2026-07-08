package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.checkpoint.BaseCheckpointSaver;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.persistence.entity.AgentGraphCheckpointExternalRefEntity;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentGraphCheckpointExternalRefRepository;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
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

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private AgentGraphCheckpointExternalRefRepository externalRefRepository;

    public SQLiteCheckpointSaver(AgentGraphCheckpointStore store) {
        this.store = store;
    }

    @Override
    public Optional<Checkpoint> get(RunnableConfig config) {
        if (config == null || config.threadId().isEmpty()) {
            return Optional.empty();
        }
        String threadId = config.threadId().orElse("");
        String runId = (String) config.context().get("runId");
        String graphName = (String) config.context().get("graphName");

        List<AgentGraphCheckpointRecord> records;
        if (runId != null && !runId.isBlank()) {
            records = store.findByRunId(runId);
        } else {
            records = store.findByThreadId(threadId);
        }

        if (graphName != null && !graphName.isBlank()) {
            records = records.stream()
                    .filter(r -> graphName.equals(r.graphName()))
                    .toList();
        }

        if (records.isEmpty()) {
            return Optional.empty();
        }
        Optional<String> checkpointId = config.checkPointId();
        if (checkpointId.isPresent() && !checkpointId.get().isBlank()) {
            for (int i = records.size() - 1; i >= 0; i--) {
                AgentGraphCheckpointRecord record = records.get(i);
                if (checkpointId.get().equals(record.checkpointId())) {
                    return Optional.of(toCheckpoint(record));
                }
            }
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
        String graphName = graphName(view.mode());
        String nextNodeId = normalizeNextNodeId(checkpoint.getNextNodeId());

        // Externalize large strings in the state map copy
        Map<String, Object> externalizedState = externalizeState(checkpoint.getState());

        AgentGraphCheckpointRecord record = new AgentGraphCheckpointRecord(
                UUID.randomUUID().toString(),
                checkpoint.getId(),
                runId == null ? "" : runId,
                threadId,
                graphName,
                checkpoint.getNodeId(),
                nextNodeId,
                summarize(checkpoint.getState(), graphName, checkpoint.getNodeId(), nextNodeId),
                externalizedState,
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
        String runId = (String) config.context().get("runId");
        String graphName = (String) config.context().get("graphName");

        List<AgentGraphCheckpointRecord> records;
        if (runId != null && !runId.isBlank()) {
            records = store.findByRunId(runId);
        } else {
            records = store.findByThreadId(threadId);
        }

        if (graphName != null && !graphName.isBlank()) {
            records = records.stream()
                    .filter(r -> graphName.equals(r.graphName()))
                    .toList();
        }
        return records.stream().map(this::toCheckpoint).toList();
    }

    @Override
    public BaseCheckpointSaver.Tag release(RunnableConfig config) {
        return new BaseCheckpointSaver.Tag("sqlite-tag", List.of());
    }

    private Checkpoint toCheckpoint(AgentGraphCheckpointRecord record) {
        Map<String, Object> restoredState = restoreState(record.fullState());
        return Checkpoint.builder()
                .id(record.checkpointId())
                .nodeId(record.nodeId())
                .nextNodeId(record.nextNodeId())
                .state(restoredState == null ? Map.of() : restoredState)
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

    private static Map<String, Object> summarize(Map<String, Object> state, String graphName, String nodeId, String nextNodeId) {
        AgentGraphStateView view = AgentGraphStateView.of(state);
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("runId", view.runId());
        summary.put("threadId", view.threadId());
        summary.put("graphName", graphName);
        summary.put("nodeId", nodeId);
        summary.put("nextNodeId", nextNodeId);
        summary.put("mode", view.mode().name());
        summary.put("modelName", view.modelName());
        summary.put("messageWindowSize", view.messageWindow().size());
        summary.put("modelStepCount", view.modelSteps().size());
        summary.put("toolCallCount", view.toolCalls().size());
        summary.put("toolResultCount", view.toolResults().size());
        summary.put("artifactCount", view.artifacts().size());
        summary.put("hasFinalAnswer", !view.finalAnswer().isBlank());
        summary.put("researchPhase", stringValue(state.get(AgentGraphStateKeys.RESEARCH_PHASE)));
        summary.put("sourceCount", intValue(state.get(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT)));
        summary.put("evidenceCount", intValue(state.get(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT)));
        summary.put("qualityGatePassed", booleanValue(state.get(AgentGraphStateKeys.QUALITY_GATE_PASSED)));
        return Map.copyOf(summary);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static int intValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Integer.parseInt(text);
            }
            catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text && !text.isBlank()) {
            return Boolean.parseBoolean(text);
        }
        return false;
    }

    private Map<String, Object> externalizeState(Map<String, Object> state) {
        if (state == null) return Map.of();
        return (Map<String, Object>) walkAndExternalize(state);
    }

    private Object walkAndExternalize(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    copy.put(key, walkAndExternalize(entry.getValue()));
                }
            }
            return copy;
        } else if (value instanceof List<?> list) {
            List<Object> copy = new java.util.ArrayList<>();
            for (Object item : list) {
                copy.add(walkAndExternalize(item));
            }
            return copy;
        } else if (value instanceof String text) {
            if (text.length() > 5000 && !text.startsWith("[EXTERNAL_REF:") && this.externalRefRepository != null) {
                String uuid = UUID.randomUUID().toString();
                this.externalRefRepository.save(new AgentGraphCheckpointExternalRefEntity(uuid, text, Instant.now()));
                return "[EXTERNAL_REF:" + uuid + "]";
            }
        }
        return value;
    }

    private Map<String, Object> restoreState(Map<String, Object> state) {
        if (state == null) return Map.of();
        return (Map<String, Object>) walkAndRestore(state);
    }

    private Object walkAndRestore(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> copy = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key) {
                    copy.put(key, walkAndRestore(entry.getValue()));
                }
            }
            return copy;
        } else if (value instanceof List<?> list) {
            List<Object> copy = new java.util.ArrayList<>();
            for (Object item : list) {
                copy.add(walkAndRestore(item));
            }
            return copy;
        } else if (value instanceof String text) {
            if (text.startsWith("[EXTERNAL_REF:") && text.endsWith("]") && this.externalRefRepository != null) {
                String uuid = text.substring("[EXTERNAL_REF:".length(), text.length() - 1);
                return this.externalRefRepository.findById(uuid)
                        .map(AgentGraphCheckpointExternalRefEntity::getContent)
                        .orElse(text);
            }
        }
        return value;
    }
}
