package org.wrj.haifa.ai.deerflow.subagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntime;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.run.RunStatus;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import reactor.core.publisher.Flux;

class SubgraphRunnerTest {

    @Test
    void timeoutCancelsChildExecutionAndReturnsTimedOutResult() {
        GraphChatRuntime graphRuntime = mock(GraphChatRuntime.class);
        RunManager runManager = mock(RunManager.class);
        RunCancellationService cancellationService = mock(RunCancellationService.class);
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        RunRecord child = new RunRecord("child-run", "thread-1", "model", RunStatus.PENDING,
                null, Map.of(), Instant.now(), Instant.now());
        when(runManager.create(any(), any(), any())).thenReturn(child);
        when(graphRuntime.run(any())).thenReturn(Flux.never());

        SubgraphRunner runner = new SubgraphRunner(graphRuntime, runManager, cancellationService,
                mock(MessageStore.class), properties, mock(ToolPolicyService.class));
        SubgraphRunner.ChildRequest request = new SubgraphRunner.ChildRequest(
                "parent-run", "parent-call", "thread-1", "model", "general-purpose", "work",
                Set.of("web_search"), List.of(), 2, 1, 1);

        SubagentResult result = runner.execute(request,
                new SubagentExecutionHook(null, "thread-1", "parent-run"));

        assertThat(result.status()).isEqualTo("TIMED_OUT");
        assertThat(result.taskId()).isEqualTo("child-run");
        verify(cancellationService).registerChild("parent-run", "child-run");
        verify(cancellationService).requestCancel("child-run", "SUBAGENT_TIMEOUT");
        verify(runManager).tryMarkFailed("child-run", "SUBAGENT_TIMEOUT");
    }
}
