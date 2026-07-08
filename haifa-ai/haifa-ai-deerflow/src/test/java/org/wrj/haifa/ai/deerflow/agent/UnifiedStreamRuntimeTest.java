package org.wrj.haifa.ai.deerflow.agent;

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
import org.wrj.haifa.ai.deerflow.thread.MessageStore;
import org.wrj.haifa.ai.deerflow.thread.ThreadManager;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.WriteTodosTool;
import org.wrj.haifa.ai.deerflow.todo.InMemoryTodoStore;
import org.wrj.haifa.ai.deerflow.todo.TodoStore;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the unified stream runtime that eliminates the hard-coded
 * streamResearch / streamChat split and relies on generic middleware
 * and tool-driven behaviour (write_todos, ask_clarification).
 */
@SpringBootTest
@ActiveProfiles("test")
class UnifiedStreamRuntimeTest {

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

    @Autowired
    private org.wrj.haifa.ai.deerflow.persistence.store.AgentClarificationStore clarificationStore;

    @Test
    void chatRequestFlowsThroughUnifiedStream() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");
        properties.setMaxIterations(4);

        AgentModelClient modelClient = prompt -> Mono.just(
                new ModelResponse("Hello from unified stream"));
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(
                properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage);

        List<AgentEvent> events = runtime.stream(new AgentRequest("thread-unified-chat",
                        "Say hello", null, List.of(), RunMode.CHAT, ResearchOptions.defaults()))
                .collectList()
                .block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_STARTED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);
        assertThat(events).anySatisfy(event -> {
            if (event.type() == AgentEventType.MODEL_COMPLETED) {
                assertThat(event.content()).contains("Hello from unified stream");
            }
        });
        String runId = events.get(0).runId();
        assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
    }

    @Test
    void researchRequestFlowsThroughUnifiedStream() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("research system");
        properties.setMaxIterations(4);
        properties.setMaxResearchSteps(4);

        AgentModelClient modelClient = prompt -> Mono.just(
                new ModelResponse("Research complete"));
        ToolRegistry tools = new ToolRegistry(List.of());
        SimpleAgentRuntime runtime = new SimpleAgentRuntime(
                properties, tools, modelClient, runManager, threadManager, messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore, toolExecutionStore,
                modelStepStore, toolCallStore, agentLoopRunStore, skillStorage,
                null, null, null, null, null, null);

        List<AgentEvent> events = runtime.stream(new AgentRequest("thread-unified-research",
                        "Analyze market trends", null, List.of(), RunMode.RESEARCH, ResearchOptions.defaults()))
                .collectList()
                .block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_STARTED,
                        AgentEventType.MODEL_COMPLETED,
                        AgentEventType.RUN_COMPLETED);
        String runId = events.get(0).runId();
        assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
    }

    @Test
    void todoQualityGateForcesContinuationUntilTodosCompleted() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");
        properties.setMaxIterations(10);
        properties.setMaxResearchSteps(10);

        TodoStore todoStore = new InMemoryTodoStore();
        WriteTodosTool writeTodosTool = new WriteTodosTool(todoStore);

        // Model step sequence:
        // 1: write_todos with pending items
        // 2: tries to answer early and is intercepted by todo quality gate
        // 3: write_todos marking items completed
        // 4: final answer accepted
        ModelToolCall tc1 = new ModelToolCall("tc-1", "write_todos",
                "{\"todos\":[{\"id\":\"1\",\"content\":\"Search sources\",\"status\":\"pending\"},{\"id\":\"2\",\"content\":\"Analyze data\",\"status\":\"pending\"}]}");
        ModelToolCall tc3 = new ModelToolCall("tc-3", "write_todos",
                "{\"todos\":[{\"id\":\"1\",\"content\":\"Search sources\",\"status\":\"completed\"},{\"id\":\"2\",\"content\":\"Analyze data\",\"status\":\"completed\"}]}");

        AgentModelClient modelClient = new AgentModelClient() {
            private int callCount = 0;
            @Override
            public Mono<ModelResponse> generate(ModelPrompt prompt) {
                callCount++;
                if (callCount == 1) {
                    return Mono.just(new ModelResponse("", List.of(tc1)));
                } else if (callCount == 2) {
                    return Mono.just(new ModelResponse("Attempted early answer"));
                } else if (callCount == 3) {
                    return Mono.just(new ModelResponse("", List.of(tc3)));
                } else {
                    return Mono.just(new ModelResponse("Final research report"));
                }
            }
        };

        SimpleAgentRuntime runtime = new SimpleAgentRuntime(
                properties,
                new ToolRegistry(List.of(writeTodosTool)),
                modelClient,
                runManager,
                threadManager,
                messageStore,
                List.of(new DynamicContextMiddleware(), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore,
                toolExecutionStore,
                modelStepStore,
                toolCallStore,
                agentLoopRunStore,
                skillStorage,
                todoStore,
                null,
                null,
                null,
                null,
                null
        );

        List<AgentEvent> events = runtime.stream(new AgentRequest("thread-todo-gate",
                        "Deep research on AI trends", null, List.of(), RunMode.RESEARCH, ResearchOptions.defaults()))
                .collectList()
                .block();

        // Should contain TODO_GATE_BLOCKED when the model tried to finish early
        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_STARTED,
                        AgentEventType.TOOL_COMPLETED,   // write_todos step 1
                        AgentEventType.TODO_GATE_BLOCKED, // todo gate intercept
                        AgentEventType.TOOL_COMPLETED,   // write_todos step 3
                        AgentEventType.MODEL_COMPLETED,  // final answer accepted
                        AgentEventType.RUN_COMPLETED);

        // The final answer should contain the actual report, not the early attempt
        assertThat(events).anySatisfy(event -> {
            if (event.type() == AgentEventType.MODEL_COMPLETED) {
                assertThat(event.content()).contains("Final research report");
            }
        });

        String runId = events.get(0).runId();
        assertThat(runManager.find(runId)).hasValueSatisfying(run ->
                assertThat(run.status()).isEqualTo(RunStatus.COMPLETED));
    }

    @Test
    void askClarificationToolSuspendsAndCanResume() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setWorkspaceRoot(".");
        properties.setSystemPrompt("test system");
        properties.setMaxIterations(2);
        properties.setMaxResearchSteps(2);

        clarificationStore.clearAll();

        ModelToolCall tc = new ModelToolCall("tc-clarify", "ask_clarification",
                "{\"question\":\"Which region?\"}");
        AgentModelClient modelClient = prompt -> Mono.just(new ModelResponse("", List.of(tc)));
        org.wrj.haifa.ai.deerflow.tool.AskClarificationTool askTool = new org.wrj.haifa.ai.deerflow.tool.AskClarificationTool(clarificationStore);

        SimpleAgentRuntime runtime = new SimpleAgentRuntime(
                properties,
                new ToolRegistry(List.of(askTool)),
                modelClient,
                runManager,
                threadManager,
                messageStore,
                List.of(new DynamicContextMiddleware(), new org.wrj.haifa.ai.deerflow.middleware.ClarificationMiddleware(clarificationStore), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore,
                toolExecutionStore,
                modelStepStore,
                toolCallStore,
                agentLoopRunStore,
                skillStorage,
                null,
                null,
                null,
                clarificationStore,
                null,
                null
        );

        List<AgentEvent> events = runtime.stream(new AgentRequest(
                        "thread-clarify-resume",
                        "Analyze market trends",
                        null,
                        List.of(),
                        RunMode.RESEARCH,
                        ResearchOptions.defaults()))
                .collectList()
                .block();

        assertThat(events).extracting(AgentEvent::type)
                .contains(AgentEventType.CLARIFICATION_REQUIRED);
        var pendingRecord = clarificationStore.findPending("thread-clarify-resume");
        assertThat(pendingRecord).isPresent();

        // Answer clarification
        clarificationStore.answer(pendingRecord.get().clarificationId(), "Asia region");

        // Resume with user clarification
        AgentModelClient resumedClient = prompt -> Mono.just(
                new ModelResponse("Resumed answer"));
        SimpleAgentRuntime resumedRuntime = new SimpleAgentRuntime(
                properties,
                new ToolRegistry(List.of(askTool)),
                resumedClient,
                runManager,
                threadManager,
                messageStore,
                List.of(new DynamicContextMiddleware(), new org.wrj.haifa.ai.deerflow.middleware.ClarificationMiddleware(clarificationStore), new TokenBudgetMiddleware(), new ToolErrorHandlingMiddleware()),
                agentEventStore,
                toolExecutionStore,
                modelStepStore,
                toolCallStore,
                agentLoopRunStore,
                skillStorage,
                null,
                null,
                null,
                clarificationStore,
                null,
                null
        );

        List<AgentEvent> resumedEvents = resumedRuntime.stream(new AgentRequest(
                        "thread-clarify-resume",
                        "Analyze market trends",
                        null,
                        List.of(),
                        RunMode.RESEARCH,
                        ResearchOptions.defaults(),
                        "default-user",
                        java.util.Map.of("clarificationId", pendingRecord.get().clarificationId())))
                .collectList()
                .block();

        assertThat(clarificationStore.findPending("thread-clarify-resume")).isEmpty();
        assertThat(resumedEvents).extracting(AgentEvent::type)
                .contains(AgentEventType.RUN_COMPLETED);
    }
}
