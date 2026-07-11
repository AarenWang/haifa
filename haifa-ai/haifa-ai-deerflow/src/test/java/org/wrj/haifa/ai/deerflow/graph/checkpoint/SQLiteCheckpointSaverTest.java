package org.wrj.haifa.ai.deerflow.graph.checkpoint;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
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
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.repository.AgentGraphCheckpointExternalRefRepository;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class SQLiteCheckpointSaverTest {

    @Autowired
    private SQLiteCheckpointSaver checkpointSaver;

    @Autowired
    private GraphChatRuntime graphChatRuntime;

    @Autowired
    private MessageStore messageStore;

    @Autowired
    private DeerFlowProperties properties;

    @Autowired
    private AgentGraphCheckpointExternalRefRepository externalRefRepository;

    @MockitoBean
    private AgentModelClient modelClient;

    @Test
    void savesAndRetrievesCheckpointDirectly() {
        String threadId = "thread-direct-" + UUID.randomUUID();
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

        Checkpoint checkpoint = Checkpoint.builder()
                .id("cp-123")
                .nodeId("node-A")
                .nextNodeId("node-B")
                .state(Map.of("runId", "run-123", "test-key", "test-val"))
                .build();

        checkpointSaver.put(config, checkpoint);

        Optional<Checkpoint> loaded = checkpointSaver.get(config);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo("cp-123");
        assertThat(loaded.get().getNodeId()).isEqualTo("node-A");
        assertThat(loaded.get().getNextNodeId()).isEqualTo("node-B");
        assertThat(loaded.get().getState()).containsEntry("test-key", "test-val");
    }

    @Test
    void reusesExternalRefsForRepeatedLargeContent() {
        String threadId = "thread-external-ref-" + UUID.randomUUID();
        String runId = "run-external-ref-" + UUID.randomUUID();
        String largeContent = ("large-content-" + UUID.randomUUID() + "-").repeat(400);
        RunnableConfig config = RunnableConfig.builder().threadId(threadId).build();

        long before = externalRefRepository.count();
        checkpointSaver.put(config, Checkpoint.builder()
                .id("cp-external-1")
                .nodeId("node-A")
                .nextNodeId("node-B")
                .state(Map.of(
                        "runId", runId,
                        "large-a", largeContent,
                        "nested", List.of(Map.of("large-b", largeContent))))
                .build());

        long afterFirstPut = externalRefRepository.count();
        assertThat(afterFirstPut - before).isEqualTo(1);

        checkpointSaver.put(config, Checkpoint.builder()
                .id("cp-external-2")
                .nodeId("node-B")
                .nextNodeId("node-C")
                .state(Map.of("runId", runId, "large-c", largeContent))
                .build());

        assertThat(externalRefRepository.count()).isEqualTo(afterFirstPut);
        Optional<Checkpoint> loaded = checkpointSaver.get(config);
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getState()).containsEntry("large-c", largeContent);
    }
    @Test
    void savesCheckpointsDuringGraphExecutionUsingSQLiteSaver() {
        // Temporarily enable checkpointing in properties
        boolean originalCheckpointEnabled = properties.getGraph().getCheckpoint().isEnabled();
        properties.getGraph().getCheckpoint().setEnabled(true);

        try {
            String runId = "run-interrupt-" + UUID.randomUUID();
            String threadId = "thread-interrupt-" + UUID.randomUUID();

            AgentRunConfig runConfig = new AgentRunConfig(
                    threadId,
                    runId,
                    "zhipu",
                    true,
                    false,
                    4,
                    Path.of("."),
                    RunMode.CHAT,
                    null,
                    Map.of()
            );

            AgentRequest agentRequest = new AgentRequest(
                    threadId,
                    "call tool list_workspace_files please",
                    "zhipu"
            );

            // Mock model response to request tool execution
            AgentLoop loop = new AgentLoop(
                    prompt -> Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-list-files", "list_workspace_files", "{}")))),
                    new ToolRegistry(List.of())
            );
            when(modelClient.generate(any()))
                    .thenReturn(
                            Mono.just(new ModelResponse("", List.of(new ModelToolCall("call-list-files", "list_workspace_files", "{}")))),
                            Mono.just(new ModelResponse("done execution")));

            GraphChatRuntimeRequest request = new GraphChatRuntimeRequest(
                    loop,
                    new LoopConfig(3, 2, 30_000, null),
                    runConfig,
                    agentRequest,
                    "You are helpful assistant",
                    "call tool list_workspace_files please",
                    new AtomicInteger(0),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );

            // Execute the graph. It should run through the chat graph while checkpointing each transition.
            List<AgentEvent> events = graphChatRuntime.run(request)
                    .collectList()
                    .block();

            assertThat(events).isNotEmpty();
            assertThat(events).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(AgentEventType.TOOL_COMPLETED);
                assertThat(event.metadata().get("status")).isEqualTo("SUCCESS");
            });
            assertThat(events).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(AgentEventType.RUN_COMPLETED);
                assertThat(event.content()).contains("done execution");
            });

            // Verify a terminal checkpoint was successfully saved in SQLite.
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
            Optional<Checkpoint> cp = checkpointSaver.get(runnableConfig);
            assertThat(cp).isPresent();
            assertThat(cp.get().getNextNodeId()).isBlank();

        } finally {
            properties.getGraph().getCheckpoint().setEnabled(originalCheckpointEnabled);
        }
    }
}
