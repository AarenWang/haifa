package org.wrj.haifa.ai.deerflow.run;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;

class RunCancellationServiceTest {

    @Test
    void completedRunCannotBeCancelledOrLeakTokenState() {
        Fixture fixture = fixture(RunStatus.COMPLETED, false);

        assertThat(fixture.service.recordCancelled("run-1", "thread-1", "USER_CANCELLED", 1)).isNull();
        assertThat(fixture.service.isCancelled("run-1")).isFalse();
        verify(fixture.runManager, never()).tryMarkCancelled(any());
        verify(fixture.eventStore, never()).save(any());
    }

    @Test
    void activeRunRecordsOneCancellationAndPreservesReason() {
        Fixture fixture = fixture(RunStatus.RUNNING, true);

        var event = fixture.service.recordCancelled("run-1", "thread-1", "TIMEOUT", 12);
        assertThat(event).isNotNull();
        assertThat(event.metadata()).containsEntry("stopReason", "TIMEOUT");
        assertThat(fixture.service.isCancelled("run-1")).isTrue();
        assertThat(fixture.service.recordCancelled("run-1", "thread-1", "USER_CANCELLED", 13)).isNull();
        verify(fixture.runManager).tryMarkCancelled("run-1");
        verify(fixture.eventStore).save(event);
    }

    private static Fixture fixture(RunStatus status, boolean transition) {
        RunManager runManager = mock(RunManager.class);
        ThreadManager threadManager = mock(ThreadManager.class);
        MessageStore messageStore = mock(MessageStore.class);
        AgentEventStore eventStore = mock(AgentEventStore.class);
        RunRecord record = new RunRecord("run-1", "thread-1", "model", status, null, Map.of(),
                Instant.now(), Instant.now());
        when(runManager.find("run-1")).thenReturn(Optional.of(record));
        when(runManager.tryMarkCancelled("run-1")).thenReturn(transition);
        return new Fixture(runManager, eventStore,
                new RunCancellationService(runManager, threadManager, messageStore, eventStore));
    }

    private record Fixture(RunManager runManager, AgentEventStore eventStore, RunCancellationService service) { }
}
