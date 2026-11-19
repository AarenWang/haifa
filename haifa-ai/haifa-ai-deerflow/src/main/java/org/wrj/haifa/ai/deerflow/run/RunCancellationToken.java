package org.wrj.haifa.ai.deerflow.run;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

/** Cooperative cancellation signal shared by coordinator, model stream and tool tasks. */
public final class RunCancellationToken {

    private final String runId;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicReference<String> reason = new AtomicReference<>("USER_CANCELLED");
    private final Sinks.Empty<Void> signal = Sinks.empty();

    public RunCancellationToken(String runId) {
        this.runId = runId == null ? "" : runId;
    }

    public boolean cancel(String cancellationReason) {
        boolean changed = cancelled.compareAndSet(false, true);
        if (changed) {
            reason.set(normalize(cancellationReason));
            signal.tryEmitEmpty();
        }
        return changed;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public String reason() {
        return reason.get();
    }

    public Mono<Void> signal() {
        return cancelled.get() ? Mono.empty() : signal.asMono();
    }

    public void throwIfCancelled() {
        if (isCancelled()) {
            throw new RunCancelledException(runId, reason());
        }
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? "USER_CANCELLED" : value.trim();
    }
}
