package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

/** Injectable run-scoped runtime context store; persisted facts remain in Graph state/database. */
@Component
public class RunExecutionContextRegistry {
    private final ConcurrentMap<String, RunExecutionContext> contexts = new ConcurrentHashMap<>();

    public void register(String runId, RunExecutionContext context) {
        if (runId != null && !runId.isBlank() && context != null) {
            contexts.put(runId, context);
        }
    }

    public Optional<RunExecutionContext> get(String runId) {
        return runId == null || runId.isBlank() ? Optional.empty() : Optional.ofNullable(contexts.get(runId));
    }

    public void close(String runId) {
        if (runId != null && !runId.isBlank()) {
            contexts.remove(runId);
        }
    }

    public int activeCount() {
        return contexts.size();
    }
}
