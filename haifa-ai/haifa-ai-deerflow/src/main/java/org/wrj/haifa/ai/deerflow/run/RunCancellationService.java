package org.wrj.haifa.ai.deerflow.run;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@Component
public class RunCancellationService {

    private static final String DEFAULT_REASON = "USER_CANCELLED";

    private final RunManager runManager;
    private final ThreadManager threadManager;
    private final MessageStore messageStore;
    private final AgentEventStore agentEventStore;
    private final Map<String, CancellationState> states = new ConcurrentHashMap<>();

    public RunCancellationService(RunManager runManager,
                                  ThreadManager threadManager,
                                  MessageStore messageStore,
                                  AgentEventStore agentEventStore) {
        this.runManager = runManager;
        this.threadManager = threadManager;
        this.messageStore = messageStore;
        this.agentEventStore = agentEventStore;
    }

    public void register(String runId, String threadId) {
        if (isBlank(runId)) {
            return;
        }
        state(runId).threadId = threadId;
    }

    public void attachTask(String runId, CompletableFuture<?> task) {
        if (isBlank(runId) || task == null) {
            return;
        }
        CancellationState state = state(runId);
        state.task = task;
        if (state.cancelled.get()) {
            task.cancel(true);
        }
    }

    public boolean requestCancel(String runId, String reason) {
        if (isBlank(runId)) {
            return false;
        }
        CancellationState state = state(runId);
        state.reason = normalizeReason(reason);
        boolean changed = state.cancelled.compareAndSet(false, true);
        if (changed) {
            state.cancelSignal.tryEmitEmpty();
        }
        CompletableFuture<?> task = state.task;
        if (task != null) {
            task.cancel(true);
        }
        return changed;
    }

    public AgentEvent recordCancelled(String runId, String threadId, String reason, long totalDurationMs) {
        if (isBlank(runId)) {
            return null;
        }
        CancellationState state = state(runId);
        if (!isBlank(threadId)) {
            state.threadId = threadId;
        }
        String resolvedThreadId = firstNonBlank(threadId, state.threadId, findThreadId(runId));
        String resolvedReason = normalizeReason(reason);
        requestCancel(runId, resolvedReason);
        if (!state.terminalRecorded.compareAndSet(false, true)) {
            return null;
        }

        RunRecord run = this.runManager.find(runId).orElse(null);
        if (run != null && isTerminal(run.status())) {
            return null;
        }

        this.runManager.markCancelled(runId);
        if (!isBlank(resolvedThreadId)) {
            this.threadManager.touch(resolvedThreadId);
            this.messageStore.add(resolvedThreadId, runId, MessageRole.SYSTEM, "Run cancelled",
                    Map.of("status", "CANCELLED", "stopReason", resolvedReason, "totalDurationMs", totalDurationMs));
        }
        AgentEvent event = AgentEvent.of(
                UUID.randomUUID().toString(),
                runId,
                resolvedThreadId == null ? "" : resolvedThreadId,
                AgentEventType.RUN_CANCELLED,
                "Run cancelled",
                Map.of("status", "CANCELLED", "stopReason", resolvedReason, "totalDurationMs", totalDurationMs)
        );
        this.agentEventStore.save(event);
        GraphEventRegistry.publish(runId, event);
        return event;
    }

    public boolean isCancellationRecorded(String runId) {
        if (isBlank(runId)) {
            return false;
        }
        CancellationState state = states.get(runId);
        return state != null && state.terminalRecorded.get();
    }

    public boolean isCancelled(String runId) {
        if (isBlank(runId)) {
            return false;
        }
        CancellationState state = states.get(runId);
        return state != null && state.cancelled.get();
    }

    public void throwIfCancelled(String runId) {
        if (isCancelled(runId)) {
            CancellationState state = states.get(runId);
            throw new RunCancelledException(runId, state == null ? DEFAULT_REASON : state.reason);
        }
    }

    public Mono<Void> cancellationSignal(String runId) {
        if (isBlank(runId)) {
            return Mono.never();
        }
        CancellationState state = state(runId);
        return state.cancelled.get() ? Mono.empty() : state.cancelSignal.asMono();
    }

    public void finishExecution(String runId) {
        if (isBlank(runId)) {
            return;
        }
        states.remove(runId);
    }

    private CancellationState state(String runId) {
        return states.computeIfAbsent(runId, key -> new CancellationState());
    }

    private String findThreadId(String runId) {
        return this.runManager.find(runId).map(RunRecord::threadId).orElse("");
    }

    private static boolean isTerminal(RunStatus status) {
        return status == RunStatus.COMPLETED || status == RunStatus.FAILED || status == RunStatus.CANCELLED;
    }

    private static String normalizeReason(String reason) {
        return isBlank(reason) ? DEFAULT_REASON : reason.trim();
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class CancellationState {
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final AtomicBoolean terminalRecorded = new AtomicBoolean(false);
        private final Sinks.Empty<Void> cancelSignal = Sinks.empty();
        private volatile String threadId;
        private volatile String reason = DEFAULT_REASON;
        private volatile CompletableFuture<?> task;
    }
}
