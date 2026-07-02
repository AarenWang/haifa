package org.wrj.haifa.ai.deerflow.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRuntime;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStatus;
import org.wrj.haifa.ai.deerflow.persistence.store.ClarificationStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.run.RunStatus;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import reactor.core.publisher.Flux;

class RunControllerClarificationTest {

    @Test
    void streamAnswersPendingClarificationAndResumesOriginalRun() {
        AgentRuntime agentRuntime = mock(AgentRuntime.class);
        RunManager runManager = mock(RunManager.class);
        ClarificationStore clarificationStore = mock(ClarificationStore.class);
        MessageStore messageStore = mock(MessageStore.class);

        RunController controller = new RunController(
                agentRuntime,
                runManager,
                mock(AgentEventStore.class),
                mock(ToolExecutionStore.class),
                mock(ToolCallStore.class),
                mock(ModelStepStore.class),
                mock(ResearchRuntimeSupport.class),
                mock(ResearchPlanStore.class),
                mock(ResearchProgressTracker.class),
                mock(ResearchQualityGate.class),
                clarificationStore,
                messageStore);

        ClarificationRecord pending = new ClarificationRecord(
                "clar-1", "thread-1", "run-1", "Which angle?", "missing_info", "",
                ClarificationStatus.PENDING, "", Instant.now(), null, List.of());
        ClarificationRecord answered = new ClarificationRecord(
                "clar-1", "thread-1", "run-1", "Which angle?", "missing_info", "",
                ClarificationStatus.ANSWERED, "Use angle A", Instant.now(), Instant.now(), List.of());
        RunRecord run = new RunRecord(
                "run-1", "thread-1", "model", RunStatus.SUSPENDED, null,
                Map.of("mode", "chat"), Instant.now(), Instant.now());
        MessageRecord originalMessage = new MessageRecord(
                "msg-1", "thread-1", "run-1", MessageRole.USER, "Original task",
                Map.of(), Instant.now());

        when(clarificationStore.findPending("thread-1")).thenReturn(Optional.of(pending));
        when(clarificationStore.answer("clar-1", "Use angle A")).thenReturn(answered);
        when(runManager.find("run-1")).thenReturn(Optional.of(run));
        when(clarificationStore.findByRunId("run-1")).thenReturn(Optional.of(answered));
        when(messageStore.listByRun("run-1")).thenReturn(List.of(originalMessage));
        when(agentRuntime.stream(any(AgentRequest.class))).thenReturn(Flux.just(
                AgentEvent.of("evt-1", "run-2", "thread-1", AgentEventType.RUN_STARTED, "started", Map.of())
        ));

        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/deerflow/runs/stream").build());
        RunCreateRequest request = new RunCreateRequest("thread-1", "Use angle A", "model");

        var response = controller.stream(request, exchange);
        var event = response.getBody().blockFirst();

        assertThat(event).isNotNull();
        assertThat(event.data().type()).isEqualTo(AgentEventType.RUN_STARTED);
        verify(clarificationStore).answer("clar-1", "Use angle A");
        verify(agentRuntime).stream(any(AgentRequest.class));
    }
}
