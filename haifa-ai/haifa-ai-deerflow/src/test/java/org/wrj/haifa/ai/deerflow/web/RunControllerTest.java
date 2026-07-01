package org.wrj.haifa.ai.deerflow.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.wrj.haifa.ai.deerflow.agent.AgentRuntime;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
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

class RunControllerTest {

    @Test
    void progressRejectsChatRun() {
        RunManager runManager = mock(RunManager.class);
        when(runManager.find("run-chat")).thenReturn(Optional.of(run("run-chat", "chat")));

        RunController controller = controller(runManager);

        assertNotFound(() -> controller.progress("run-chat").block());
    }

    @Test
    void qualityGateRejectsChatRun() {
        RunManager runManager = mock(RunManager.class);
        when(runManager.find("run-chat")).thenReturn(Optional.of(run("run-chat", "chat")));

        RunController controller = controller(runManager);

        assertNotFound(() -> controller.qualityGate("run-chat").block());
    }

    private static RunController controller(RunManager runManager) {
        return new RunController(
                mock(AgentRuntime.class),
                runManager,
                mock(AgentEventStore.class),
                mock(ToolExecutionStore.class),
                mock(ToolCallStore.class),
                mock(ModelStepStore.class),
                mock(ResearchRuntimeSupport.class),
                mock(ResearchPlanStore.class),
                mock(ResearchProgressTracker.class),
                mock(ResearchQualityGate.class)
        );
    }

    private static RunRecord run(String runId, String mode) {
        Instant now = Instant.now();
        return new RunRecord(runId, "thread-1", "model", RunStatus.COMPLETED, null,
                Map.of("mode", mode), now, now);
    }

    private static void assertNotFound(Runnable request) {
        assertThatThrownBy(request::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, ex ->
                        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
