package org.wrj.haifa.ai.deerflow.run;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class RunManager {

    private final Map<String, RunRecord> runs = new ConcurrentHashMap<>();

    public RunRecord create(String threadId, String modelName, Map<String, Object> metadata) {
        String runId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        RunRecord record = new RunRecord(runId, threadId, modelName, RunStatus.PENDING, null,
                metadata == null ? Map.of() : Map.copyOf(metadata), now, now);
        this.runs.put(runId, record);
        return record;
    }

    public Optional<RunRecord> find(String runId) {
        return Optional.ofNullable(this.runs.get(runId));
    }

    public RunRecord markRunning(String runId) {
        return updateStatus(runId, RunStatus.RUNNING);
    }

    public RunRecord markCompleted(String runId) {
        return updateStatus(runId, RunStatus.COMPLETED);
    }

    public RunRecord markCancelled(String runId) {
        return updateStatus(runId, RunStatus.CANCELLED);
    }

    public RunRecord markFailed(String runId, String error) {
        return this.runs.computeIfPresent(runId, (ignored, existing) -> existing.withError(error));
    }

    private RunRecord updateStatus(String runId, RunStatus status) {
        return this.runs.computeIfPresent(runId, (ignored, existing) -> existing.withStatus(status));
    }

    public int count() {
        return this.runs.size();
    }
}
