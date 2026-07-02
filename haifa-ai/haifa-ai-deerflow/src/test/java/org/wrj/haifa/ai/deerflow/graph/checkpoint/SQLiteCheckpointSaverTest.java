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
    void interruptsAndResumesGraphExecutionUsingSQLiteSaver() {
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
                    prompt -> Mono.just(new ModelResponse("<tool_call name=\"list_workspace_files\">{}</tool_call>")),
                    new ToolRegistry(List.of())
            );
            when(modelClient.generate(any()))
                    .thenReturn(
                            Mono.just(new ModelResponse("<tool_call name=\"list_workspace_files\">{}</tool_call>")),
                            Mono.just(new ModelResponse("<final_answer>done execution</final_answer>")));

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

            // Execute the graph. It should run load_context -> apply_prompt_middlewares -> call_model -> parse_model_output,
            // then suspend before execute_tools!
            List<AgentEvent> events = graphChatRuntime.run(request)
                    .collectList()
                    .block();

            assertThat(events).isNotEmpty();

            // Verify a checkpoint was successfully saved in SQLite and the next node is execute_tools
            RunnableConfig runnableConfig = RunnableConfig.builder().threadId(threadId).build();
            Optional<Checkpoint> cp = checkpointSaver.get(runnableConfig);
            assertThat(cp).isPresent();
            assertThat(cp.get().getNextNodeId()).isEqualTo("execute_tools");

            // Now, mock the model response for the resume phase to return final answer
            AgentLoop resumeLoop = new AgentLoop(
                    prompt -> Mono.just(new ModelResponse("<final_answer>done execution</final_answer>")),
                    new ToolRegistry(List.of())
            );

            String nextRunId = "run-interrupt-resume-" + UUID.randomUUID();
            AgentRunConfig nextRunConfig = new AgentRunConfig(
                    threadId,
                    nextRunId,
                    "zhipu",
                    true,
                    false,
                    4,
                    Path.of("."),
                    RunMode.CHAT,
                    null,
                    Map.of()
            );

            GraphChatRuntimeRequest resumeRequest = new GraphChatRuntimeRequest(
                    resumeLoop,
                    new LoopConfig(3, 2, 30_000, null),
                    nextRunConfig,
                    agentRequest,
                    "You are helpful assistant",
                    "call tool list_workspace_files please",
                    new AtomicInteger(0),
                    null,
                    List.of(),
                    List.of(),
                    List.of()
            );

            // Resume the graph. It should run execute_tools -> call_model -> parse_model_output -> finalize.
            List<AgentEvent> resumeEvents = graphChatRuntime.run(resumeRequest)
                    .collectList()
                    .block();

            assertThat(resumeEvents).isNotEmpty();

            // Verify it completed with final answer
            assertThat(resumeEvents).anySatisfy(event -> {
                assertThat(event.type()).isEqualTo(AgentEventType.RUN_COMPLETED);
                assertThat(event.content()).contains("done execution");
            });

            // The checkpoint should have no nextNodeId now (meaning graph finished)
            Optional<Checkpoint> finalCp = checkpointSaver.get(runnableConfig);
            assertThat(finalCp).isPresent();
            assertThat(finalCp.get().getNextNodeId()).isBlank();

        } finally {
            properties.getGraph().getCheckpoint().setEnabled(originalCheckpointEnabled);
        }
    }
}
