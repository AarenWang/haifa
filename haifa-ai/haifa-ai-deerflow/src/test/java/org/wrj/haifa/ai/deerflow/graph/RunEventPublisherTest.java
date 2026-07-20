package org.wrj.haifa.ai.deerflow.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import reactor.core.publisher.Sinks;

class RunEventPublisherTest {

    @Test
    void concurrentWritersAreSerializedAndSequenced() throws Exception {
        RunEventPublisher publisher = new RunEventPublisher();
        Sinks.Many<AgentEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        publisher.register("run-events", sink, new AtomicInteger());
        List<AgentEvent> received = new ArrayList<>();
        sink.asFlux().subscribe(received::add);

        int count = 30;
        ExecutorService workers = Executors.newFixedThreadPool(6);
        CountDownLatch ready = new CountDownLatch(6);
        CountDownLatch start = new CountDownLatch(1);
        for (int i = 0; i < count; i++) {
            workers.execute(() -> {
                ready.countDown();
                await(start);
                publisher.publish("run-events", event(AgentEventType.MODEL_DELTA));
            });
        }
        assertThat(ready.await(1, TimeUnit.SECONDS)).isTrue();
        start.countDown();
        workers.shutdown();
        assertThat(workers.awaitTermination(2, TimeUnit.SECONDS)).isTrue();
        publisher.complete("run-events");

        assertThat(received).hasSize(count);
        assertThat(received.stream().map(event -> (Integer) event.metadata().get("eventSequence")).toList())
                .containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, count).boxed().toList());
        assertThat(publisher.emitFailureCount()).isZero();
        publisher.deregister("run-events");
        assertThat(publisher.activeRunCount()).isZero();
    }

    @Test
    void terminalConflictAndClosedSinkAreObservable() {
        RunEventPublisher publisher = new RunEventPublisher();
        Sinks.Many<AgentEvent> sink = Sinks.many().unicast().onBackpressureBuffer();
        publisher.register("run-terminal", sink, new AtomicInteger());
        sink.asFlux().subscribe();

        assertThat(publisher.publish("run-terminal", event(AgentEventType.RUN_COMPLETED)))
                .isEqualTo(RunEventPublisher.PublishOutcome.PUBLISHED);
        assertThat(publisher.publish("run-terminal", event(AgentEventType.RUN_CANCELLED)))
                .isEqualTo(RunEventPublisher.PublishOutcome.DUPLICATE_TERMINAL);
        assertThat(publisher.terminalConflictCount()).isEqualTo(1);
        publisher.complete("run-terminal");
        assertThat(publisher.publish("run-terminal", event(AgentEventType.MODEL_DELTA)))
                .isEqualTo(RunEventPublisher.PublishOutcome.CLOSED);
        assertThat(publisher.emitFailureCount()).isEqualTo(1);
        assertThat(publisher.publish("missing", event(AgentEventType.MODEL_DELTA)))
                .isEqualTo(RunEventPublisher.PublishOutcome.NO_CHANNEL);
        assertThat(publisher.emitFailureCount()).isEqualTo(2);
        publisher.deregister("run-terminal");
        assertThat(publisher.activeRunCount()).isZero();
    }

    @Test
    void missingSseChannelFallsBackToAuditPersistence() {
        AgentEventStore store = mock(AgentEventStore.class);
        RunEventPublisher publisher = new RunEventPublisher(store);
        AgentEvent event = event(AgentEventType.RUN_CANCELLED);

        assertThat(publisher.publish("disconnected", event))
                .isEqualTo(RunEventPublisher.PublishOutcome.NO_CHANNEL);
        verify(store).save(event);
    }

    private static AgentEvent event(AgentEventType type) {
        return AgentEvent.of(UUID.randomUUID().toString(), "run-events", "thread", type, type.name(), Map.of());
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await(1, TimeUnit.SECONDS);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
