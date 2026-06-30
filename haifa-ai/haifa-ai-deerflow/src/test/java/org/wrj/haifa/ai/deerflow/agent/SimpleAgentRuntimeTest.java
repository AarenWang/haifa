package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.DynamicContextMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.TokenBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.ToolErrorHandlingMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolExecutionStore;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunStatus;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.tool.CurrentTimeTool;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ListWorkspaceFilesTool;
import org.wrj.haifa.ai.deerflow.tool.ReadWorkspaceFileTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SimpleAgentRuntimeTest {

    @Autowired
    private RunManager runManager;

    @Autowired
    private ThreadManager threadManager;

    @Autowired
    private MessageStore messageStore;

    @Autowired
    private AgentEventStore agentEventStore;

    @Autowired
    private ToolExecutionStore toolExecutionStore;

    @Autowired
    private ModelStepStore modelStepStore;

    @Autowired
    private ToolCallStore toolCallStore;

    @Autowired
    private AgentLoopRunStore agentLoopRunStore;

    @Autowired
    private org.wrj.haifa.ai.deerflow.skill.SkillStorage skillStorage;

    @Test
    void streamsToolAndModelEvents() throws Exception {
        Path workspace = Files.createTempDirectory("deerflow-runtime-test");
        Files.writeString(workspace.resolve("note.md"), "hello deerflow");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(workspace.toString());
        properties.setSystemPrompt("test system");
        properties.setMaxIterations(4);

        org.wrj.haifa.ai.deerflow.model.ModelToolCall tc1 = new org.wrj.haifa.ai.deerflow.model.ModelToolCall("tc-1", "list_workspace_files", "{}");
        org.wrj.haifa.ai.deerflow.model.ModelToolCall tc2 = new org.wrj.haifa.ai.deerflow.model.ModelToolCall("tc-2", "read_workspace_file", "{\"path\":\"note.md\"}");

        AgentModelClient modelClient = new AgentModelClient() {
            private int callCount = 0;
            @Override
            public Mono<ModelResponse> generate(ModelPrompt prompt) {
                callCount++;
                if (callCount == 1) {
                    return Mono.just(new ModelResponse("", List.of(tc1)));
                } else if (callCount == 2) {
                    return Mono.just(new ModelResponse("", List.of(tc2)));
                } else {
                    return Mono.just(new ModelResponse("<final_answer>I read note.md: hello deerflow</final_answer>"));
                }
            }
        };
        ToolRegistry tools = new ToolRegistry(List.of(
                new CurrentTimeTool(),
                new ListWorkspaceFilesTool(),
                new ReadWorkspaceFileTool()));
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage);

        List<AgentEvent> events = runtime.stream(new AgentRequest("thread-1",
                        "List workspace files and read \"note.md\"", null))
                .collectList()
                .block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_STARTED,
                        AgentEventType.TOOL_STARTED,
                        AgentEventType.TOOL_COMPLETED,
                        AgentEventType.MODEL_STARTED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);
        assertThat(events).anySatisfy(event -> assertThat(event.content()).contains("note.md"));
        assertThat(events).anySatisfy(event -> assertThat(event.content()).contains("hello deerflow"));

        String runId = events.get(0).runId();
        assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
        assertThat(threadManager.find("thread-1")).isPresent();
        assertThat(messageStore.listByThread("thread-1")).extracting(MessageRecord::role)
                .contains(MessageRole.USER, MessageRole.TOOL, MessageRole.ASSISTANT);

        // Verify persistence
        assertThat(agentEventStore.findByRunId(runId)).isNotEmpty();
        assertThat(toolExecutionStore.findByRunId(runId)).isNotEmpty();
    }

    @Test
    void marksRunFailedWhenModelFails() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");

        AgentModelClient modelClient = prompt -> Mono.error(new IllegalStateException("model down"));
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage);

        StepVerifier.create(runtime.stream(new AgentRequest("thread-2", "hello", null)))
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(3)
                .consumeRecordedWith(events -> {
                    assertThat(events).extracting(AgentEvent::type)
                            .containsExactly(AgentEventType.RUN_STARTED,
                                    AgentEventType.MODEL_STARTED,
                                    AgentEventType.RUN_FAILED);
                    String runId = events.iterator().next().runId();
                    assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                            assertThat(run.status()).isEqualTo(RunStatus.FAILED));
                    assertThat(messageStore.listByThread("thread-2")).extracting(MessageRecord::role)
                            .contains(MessageRole.USER, MessageRole.SYSTEM);
                    assertThat(agentEventStore.findByRunId(runId)).isNotEmpty();
                })
                .verifyComplete();
    }

    @Test
    void convertsToolFailureToObservationAndContinues() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");

        org.wrj.haifa.ai.deerflow.model.ModelToolCall tc = new org.wrj.haifa.ai.deerflow.model.ModelToolCall("tc-expl", "explode", "{}");
        AgentModelClient modelClient = new AgentModelClient() {
            private int callCount = 0;
            @Override
            public Mono<ModelResponse> generate(ModelPrompt prompt) {
                callCount++;
                if (callCount == 1) {
                    return Mono.just(new ModelResponse("", List.of(tc)));
                } else {
                    return Mono.just(new ModelResponse("<final_answer>handled gracefully</final_answer>"));
                }
            }
        };
        ToolRegistry tools = new ToolRegistry(List.of(new ExplodingTool()));
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage);

        List<AgentEvent> events = runtime.stream(new AgentRequest("thread-3", "please explode", null))
                .collectList()
                .block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.TOOL_COMPLETED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);
        assertThat(events).anySatisfy(event -> assertThat(event.content()).contains("Tool failed: boom"));
        assertThat(events).anySatisfy(event -> {
            if (event.type() == AgentEventType.MODEL_COMPLETED) {
                assertThat(event.content()).contains("handled gracefully");
            }
        });
        assertThat(runManager.find(events.get(0).runId())).hasValueSatisfying(run ->
                assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
        assertThat(messageStore.listByThread("thread-3")).extracting(MessageRecord::role)
                .contains(MessageRole.USER, MessageRole.TOOL, MessageRole.ASSISTANT);
    }

    @Test
    void producesControlledEventWhenBudgetExceeded() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");
        properties.setCharBudget(5);

        AgentModelClient modelClient = prompt -> Mono.just(new ModelResponse("should not reach model"));
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage);

        StepVerifier.create(runtime.stream(new AgentRequest("thread-4", "this is a long message", null)))
                .recordWith(java.util.ArrayList::new)
                .expectNextCount(3)
                .consumeRecordedWith(events -> {
                    assertThat(events).extracting(AgentEvent::type)
                            .containsExactly(AgentEventType.RUN_STARTED,
                                    AgentEventType.MODEL_COMPLETED,
                                    AgentEventType.RUN_COMPLETED);
                    assertThat(events).anySatisfy(event ->
                            assertThat(event.content()).contains("Budget exceeded"));
                    String runId = events.iterator().next().runId();
                    assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                            assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
                })
                .verifyComplete();
    }

    private static final class ExplodingTool implements AgentTool {

        @Override
        public String name() {
            return "explode";
        }

        @Override
        public String description() {
            return "Throws for testing.";
        }

        @Override
        public boolean supports(String userMessage) {
            return true;
        }

        @Override
        public ToolResult execute(ToolRequest request) {
            throw new IllegalStateException("boom");
        }
    }
}
