package org.wrj.haifa.ai.deerflow.graph;

import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Registry to bridge asynchronous graph node execution with the reactive event sink.
 * Uses runId to route events correctly without polluting the graph state map.
 */
public final class GraphEventRegistry {

    private static volatile RunEventPublisher publisher = new RunEventPublisher(false);

    private GraphEventRegistry() {
    }

    public static void register(String runId, Sinks.Many<AgentEvent> sink, AtomicInteger seq) {
        publisher.register(runId, sink, seq);
    }

    public static void deregister(String runId) {
        publisher.deregister(runId);
    }

    public static RunEventPublisher.PublishOutcome publish(String runId, AgentEvent event) {
        return publisher.publish(runId, event);
    }

    public static int nextSeq(String runId) {
        return publisher.nextSequence(runId);
    }

    public static Sinks.EmitResult complete(String runId) {
        return publisher.complete(runId);
    }

    public static Sinks.EmitResult error(String runId, Throwable error) {
        return publisher.error(runId, error);
    }

    public static int activeRunCount() {
        return publisher.activeRunCount();
    }

    static void install(RunEventPublisher replacement) {
        if (replacement != null) {
            publisher = replacement;
        }
    }
}
