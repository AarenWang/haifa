package org.wrj.haifa.ai.deerflow.graph;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class GraphChatLifecycleRegistry {

    private static final ConcurrentMap<String, GraphChatLifecycleContext> CONTEXTS = new ConcurrentHashMap<>();

    private GraphChatLifecycleRegistry() {
    }

    public static void register(String runId, GraphChatLifecycleContext context) {
        if (runId == null || runId.isBlank() || context == null) {
            return;
        }
        CONTEXTS.put(runId, context);
    }

    public static Optional<GraphChatLifecycleContext> get(String runId) {
        if (runId == null || runId.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(CONTEXTS.get(runId));
    }

    public static void deregister(String runId) {
        if (runId == null || runId.isBlank()) {
            return;
        }
        CONTEXTS.remove(runId);
    }
}