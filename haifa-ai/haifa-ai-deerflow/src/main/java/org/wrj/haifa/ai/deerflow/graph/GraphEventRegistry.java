package org.wrj.haifa.ai.deerflow.graph;

import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry to bridge asynchronous graph node execution with the reactive event sink.
 * Uses runId to route events correctly without polluting the graph state map.
 */
public final class GraphEventRegistry {

    private static final Map<String, Sinks.Many<AgentEvent>> sinks = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> sequences = new ConcurrentHashMap<>();

    private GraphEventRegistry() {
    }

    public static void register(String runId, Sinks.Many<AgentEvent> sink, AtomicInteger seq) {
        if (runId != null && sink != null && seq != null) {
            sinks.put(runId, sink);
            sequences.put(runId, seq);
        }
    }

    public static void deregister(String runId) {
        if (runId != null) {
            sinks.remove(runId);
            sequences.remove(runId);
        }
    }

    public static void publish(String runId, AgentEvent event) {
        if (runId != null && event != null) {
            Sinks.Many<AgentEvent> sink = sinks.get(runId);
            if (sink != null) {
                sink.tryEmitNext(event);
            }
        }
    }

    public static int nextSeq(String runId) {
        if (runId == null) {
            return 0;
        }
        AtomicInteger seq = sequences.get(runId);
        return seq != null ? seq.incrementAndGet() : 0;
    }
}
