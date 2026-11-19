package org.wrj.haifa.ai.deerflow.persistence.store;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ToolExecutionStoreTest {

    @Autowired
    private ToolExecutionStore toolExecutionStore;

    @Autowired
    private RunManager runManager;

    @Test
    void savesToolExecutionLifecycle() {
        RunRecord run = runManager.create("thread-tools", "model", Map.of());
        String runId = run.runId();
        String threadId = run.threadId();

        toolExecutionStore.saveStarted(runId, threadId, "test_tool", "A test tool", "{}", Map.of("input", "hello"));
        toolExecutionStore.saveCompleted(runId, threadId, "test_tool", "result", 100L, Map.of("output", "done"));

        var executions = toolExecutionStore.findByRunId(runId);
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getToolName()).isEqualTo("test_tool");
        assertThat(executions.get(0).getStatus()).isEqualTo(org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity.Status.COMPLETED);
        assertThat(executions.get(0).getOutput()).isEqualTo("result");
    }

    @Test
    void savesFailedToolExecution() {
        RunRecord run = runManager.create("thread-fail", "model", Map.of());
        String runId = run.runId();
        String threadId = run.threadId();

        toolExecutionStore.saveStarted(runId, threadId, "fail_tool", "A failing tool", "{}", Map.of());
        toolExecutionStore.saveFailed(runId, threadId, "fail_tool", "Tool failed: network error", 50L, Map.of());

        var executions = toolExecutionStore.findByRunId(runId);
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getStatus()).isEqualTo(org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity.Status.FAILED);
        assertThat(executions.get(0).getError()).contains("network error");
    }

    @Test
    void savesDeniedToolExecution() {
        RunRecord run = runManager.create("thread-deny", "model", Map.of());
        String runId = run.runId();
        String threadId = run.threadId();

        toolExecutionStore.saveDenied(runId, threadId, "dangerous_tool", "Policy denied", Map.of("denied", true));

        var executions = toolExecutionStore.findByRunId(runId);
        assertThat(executions).hasSize(1);
        assertThat(executions.get(0).getStatus()).isEqualTo(org.wrj.haifa.ai.deerflow.persistence.entity.ToolExecutionEntity.Status.DENIED);
    }
}
