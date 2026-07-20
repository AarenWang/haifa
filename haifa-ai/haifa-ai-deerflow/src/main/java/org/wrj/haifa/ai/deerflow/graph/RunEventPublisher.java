package org.wrj.haifa.ai.deerflow.graph;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import reactor.core.publisher.Sinks;

/**
 * Per-run single writer for Graph events. The registry facade is retained only so existing
 * graph nodes do not need a flag-day constructor migration.
 */
@Component
public class RunEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(RunEventPublisher.class);
    private final Map<String, RunChannel> channels = new ConcurrentHashMap<>();
    private final AtomicLong emitFailures = new AtomicLong();
    private final AtomicLong terminalConflicts = new AtomicLong();
    private final AgentEventStore auditStore;

    public RunEventPublisher() {
        this(null, false);
    }

    @Autowired
    public RunEventPublisher(@Autowired(required = false) AgentEventStore auditStore) {
        this(auditStore, true);
    }

    private RunEventPublisher(AgentEventStore auditStore, boolean install) {
        this.auditStore = auditStore;
        if (install) {
            GraphEventRegistry.install(this);
        }
    }

    RunEventPublisher(boolean install) {
        this(null, install);
    }

    public void register(String runId, Sinks.Many<AgentEvent> sink, AtomicInteger sequence) {
        if (isBlank(runId) || sink == null || sequence == null) {
            return;
        }
        RunChannel previous = channels.put(runId, new RunChannel(sink, sequence));
        if (previous != null) {
            log.warn("Replacing active event channel. runId={}", runId);
        }
    }

    public PublishOutcome publish(String runId, AgentEvent event) {
        if (isBlank(runId) || event == null) {
            return PublishOutcome.INVALID;
        }
        RunChannel channel = channels.get(runId);
        if (channel == null) {
            emitFailures.incrementAndGet();
            persistFallback(event);
            log.warn("Event has no active SSE channel; audit fallback attempted when configured. runId={}, eventId={}, type={}",
                    runId, event.eventId(), event.type());
            return PublishOutcome.NO_CHANNEL;
        }
        synchronized (channel.monitor) {
            if (channel.closed) {
                emitFailures.incrementAndGet();
                persistFallback(event);
                log.warn("Event rejected after publisher close. runId={}, eventId={}, type={}", runId, event.eventId(), event.type());
                return PublishOutcome.CLOSED;
            }
            if (isTerminal(event.type()) && channel.terminalType != null) {
                if (channel.terminalType != event.type()) {
                    terminalConflicts.incrementAndGet();
                    log.warn("Conflicting terminal event suppressed. runId={}, existing={}, attempted={}",
                            runId, channel.terminalType, event.type());
                }
                return PublishOutcome.DUPLICATE_TERMINAL;
            }
            int sequence = reserveSequence(channel.sequence, event.eventId());
            AgentEvent sequenced = withSequence(event, sequence);
            Sinks.EmitResult result = channel.sink.tryEmitNext(sequenced);
            if (result.isFailure()) {
                emitFailures.incrementAndGet();
                persistFallback(event);
                log.error("Event emission failed. runId={}, eventId={}, type={}, sequence={}, result={}",
                        runId, event.eventId(), event.type(), sequence, result);
                return PublishOutcome.EMIT_FAILED;
            }
            if (isTerminal(event.type())) {
                channel.terminalType = event.type();
            }
            return PublishOutcome.PUBLISHED;
        }
    }

    public int nextSequence(String runId) {
        RunChannel channel = channels.get(runId);
        return channel == null ? 0 : channel.sequence.incrementAndGet();
    }

    public Sinks.EmitResult complete(String runId) {
        RunChannel channel = channels.get(runId);
        if (channel == null) {
            return Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER;
        }
        synchronized (channel.monitor) {
            if (channel.closed) {
                return Sinks.EmitResult.FAIL_TERMINATED;
            }
            Sinks.EmitResult result = channel.sink.tryEmitComplete();
            if (result.isSuccess() || result == Sinks.EmitResult.FAIL_TERMINATED) {
                channel.closed = true;
            } else {
                emitFailures.incrementAndGet();
                log.error("Event publisher completion failed. runId={}, result={}", runId, result);
            }
            return result;
        }
    }

    public Sinks.EmitResult error(String runId, Throwable error) {
        RunChannel channel = channels.get(runId);
        if (channel == null) {
            return Sinks.EmitResult.FAIL_ZERO_SUBSCRIBER;
        }
        synchronized (channel.monitor) {
            if (channel.closed) {
                return Sinks.EmitResult.FAIL_TERMINATED;
            }
            Sinks.EmitResult result = channel.sink.tryEmitError(error);
            if (result.isSuccess() || result == Sinks.EmitResult.FAIL_TERMINATED) {
                channel.closed = true;
            } else {
                emitFailures.incrementAndGet();
                log.error("Event publisher error emission failed. runId={}, result={}", runId, result, error);
            }
            return result;
        }
    }

    public void deregister(String runId) {
        if (!isBlank(runId)) {
            channels.remove(runId);
        }
    }

    public int activeRunCount() {
        return channels.size();
    }

    public long emitFailureCount() {
        return emitFailures.get();
    }

    public long terminalConflictCount() {
        return terminalConflicts.get();
    }

    private static AgentEvent withSequence(AgentEvent event, int sequence) {
        Map<String, Object> metadata = new LinkedHashMap<>(event.metadata());
        metadata.put("eventSequence", sequence);
        return new AgentEvent(event.eventId(), event.runId(), event.threadId(), event.type(), event.content(),
                metadata, event.createdAt(), event.protocolState());
    }

    private void persistFallback(AgentEvent event) {
        if (auditStore == null) {
            return;
        }
        try {
            auditStore.save(event);
        }
        catch (RuntimeException ex) {
            log.error("Failed to persist event after SSE emission failure. runId={}, eventId={}, type={}",
                    event.runId(), event.eventId(), event.type(), ex);
        }
    }

    private static int reserveSequence(AtomicInteger sequence, String eventId) {
        try {
            int reserved = Integer.parseInt(eventId);
            sequence.accumulateAndGet(reserved, Math::max);
            return reserved;
        }
        catch (RuntimeException ignored) {
            return sequence.incrementAndGet();
        }
    }

    private static boolean isTerminal(AgentEventType type) {
        return type == AgentEventType.RUN_COMPLETED || type == AgentEventType.RUN_FAILED
                || type == AgentEventType.RUN_CANCELLED;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public enum PublishOutcome {
        PUBLISHED,
        NO_CHANNEL,
        CLOSED,
        DUPLICATE_TERMINAL,
        EMIT_FAILED,
        INVALID
    }

    private static final class RunChannel {
        private final Object monitor = new Object();
        private final Sinks.Many<AgentEvent> sink;
        private final AtomicInteger sequence;
        private AgentEventType terminalType;
        private boolean closed;

        private RunChannel(Sinks.Many<AgentEvent> sink, AtomicInteger sequence) {
            this.sink = sink;
            this.sequence = sequence;
        }
    }
}
