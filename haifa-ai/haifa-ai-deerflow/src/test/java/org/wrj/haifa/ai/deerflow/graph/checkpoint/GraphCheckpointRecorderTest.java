package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntime;
import org.wrj.haifa.ai.deerflow.graph.GraphChatRuntimeRequest;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class GraphCheckpointRecorderTest {

    @Autowired
    private AgentGraphCheckpointStore checkpointStore;

    @Autowired
    private DeerFlowProperties properties;

    @Autowired
    private GraphChatRuntime runtime;

    @MockitoBean
    private AgentModelClient modelClient;

    @Test
    void activeChatRecordsSQLiteCheckpointsWhenEnabled() {
        properties.getGraph().getCheckpoint().setEnabled(true);
        when(modelClient.generate(any()))
                .thenReturn(Mono.just(new ModelResponse("<final_answer>done</final_answer>")));

        String runId = "run-checkpoint-" + System.nanoTime();
        String threadId = "thread-checkpoint-" + System.nanoTime();
        AgentRunConfig runConfig = new AgentRunConfig(threadId, runId, "model", false, false,
                4, Path.of("."), RunMode.CHAT, null, Map.of());
        AgentRequest agentRequest = new AgentRequest(threadId, "hello", "model");
        AgentLoop loop = new AgentLoop(prompt -> Mono.just(new ModelResponse("<final_answer>done</final_answer>")),
                new ToolRegistry(List.of()));

        runtime.run(new GraphChatRuntimeRequest(
                        loop,
                        new LoopConfig(2, 2, 30_000, null),
                        runConfig,
                        agentRequest,
                        "system",
                        "user",
                        new AtomicInteger(),
                        null,
                        List.of(),
                        List.of(),
                        List.of()
                ))
                .collectList()
                .block();

        List<AgentGraphCheckpointRecord> byRun = checkpointStore.findByRunId(runId);
        assertThat(byRun).isNotEmpty();
        assertThat(byRun).allSatisfy(record -> {
            assertThat(record.threadId()).isEqualTo(threadId);
            assertThat(record.graphName()).isEqualTo("haifa-active-chat");
            assertThat(record.stateSummary()).containsEntry("runId", runId);
            assertThat(record.stateSummary()).containsEntry("threadId", threadId);
            assertThat(record.stateSummary()).containsEntry("mode", "CHAT");
            assertThat(record.fullState()).containsEntry("runId", runId);
            assertThat(record.fullState()).containsEntry("threadId", threadId);
            assertThat(record.fullState()).containsEntry("mode", "CHAT");
        });
        assertThat(checkpointStore.findByThreadId(threadId)).hasSameSizeAs(byRun);
    }
}
