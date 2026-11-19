package org.wrj.haifa.ai.deerflow.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;

class DefaultAgentLoopObserverTest {

    @Test
    void rejectsDownloadableFileClaimWithoutToolExecution() {
        DefaultAgentLoopObserver observer = new DefaultAgentLoopObserver(null);
        AgentRunConfig config = new AgentRunConfig(
                "thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), Map.of());

        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                config,
                "已为您生成标准 .eml 文件，文件已保存至 D:\\workspace\\haifa\\outputs\\report.eml，供下载。",
                List.of(),
                new AtomicInteger(),
                1,
                0);

        assertThat(decision.accepted()).isFalse();
        assertThat(decision.metadata()).containsEntry("reason", "artifact_claim_without_tool");
        assertThat(decision.retryInstruction()).contains("write_file", "outputs/");
    }

    @Test
    void acceptsDownloadableFileClaimAfterToolExecution() {
        DefaultAgentLoopObserver observer = new DefaultAgentLoopObserver(null);
        AgentRunConfig config = new AgentRunConfig(
                "thread-1", "run-1", "test-model", false, false,
                4, Path.of("."), Map.of());

        FinalAnswerDecision decision = observer.onFinalAnswerProposed(
                config,
                "已生成 .eml 文件，可从 artifact 下载。",
                List.of(),
                new AtomicInteger(),
                1,
                1);

        assertThat(decision.accepted()).isTrue();
    }
}
