package org.wrj.haifa.ai.deerflow.agent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.DynamicContextMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.TokenBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.middleware.ToolErrorHandlingMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunStatus;
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

class SimpleAgentRuntimeTest {

    @Test
    void streamsToolAndModelEvents() throws Exception {
        Path workspace = Files.createTempDirectory("deerflow-runtime-test");
        Files.writeString(workspace.resolve("note.md"), "hello deerflow");

        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(workspace.toString());
        properties.setSystemPrompt("test system");
        properties.setMaxIterations(4);

        AgentModelClient modelClient = prompt -> Mono.just("model saw: " + prompt.userPrompt());
        RunManager runManager = new RunManager();
        ToolRegistry tools = new ToolRegistry(List.of(
                new CurrentTimeTool(),
                new ListWorkspaceFilesTool(),
                new ReadWorkspaceFileTool()));
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()));

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
    }

    @Test
    void marksRunFailedWhenModelFails() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");

        AgentModelClient modelClient = prompt -> Mono.error(new IllegalStateException("model down"));
        RunManager runManager = new RunManager();
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()));

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
                })
                .verifyComplete();
    }

    @Test
    void convertsToolFailureToObservationAndContinues() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");

        AgentModelClient modelClient = prompt -> Mono.just(prompt.userPrompt());
        RunManager runManager = new RunManager();
        ToolRegistry tools = new ToolRegistry(List.of(new ExplodingTool()));
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()));

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
    }

    @Test
    void producesControlledEventWhenBudgetExceeded() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");
        properties.setCharBudget(5);

        AgentModelClient modelClient = prompt -> Mono.just("should not reach model");
        RunManager runManager = new RunManager();
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(properties, tools, modelClient, runManager,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()));

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
