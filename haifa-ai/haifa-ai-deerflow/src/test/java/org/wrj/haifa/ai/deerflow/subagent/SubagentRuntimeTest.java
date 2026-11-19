package org.wrj.haifa.ai.deerflow.subagent;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolCall;
import org.wrj.haifa.ai.deerflow.agent.lifecycle.ExecutionToolResult;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.SearchIngestionResult;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SubagentRuntimeTest {

    private AgentModelClient modelClient;
    private ResearchRuntimeSupport researchRuntimeSupport;
    private SubagentRegistry subagentRegistry;
    private DeerFlowProperties properties;
    private ToolOutputBudgetMiddleware budgetMiddleware;
    private ApplicationContext applicationContext;
    private ToolPolicyService toolPolicyService;
    private ToolRegistry toolRegistry;
    private SubgraphRunner subgraphRunner;

    private SubagentRuntime subagentRuntime;

    @BeforeEach
    void setUp() {
        modelClient = mock(AgentModelClient.class);
        researchRuntimeSupport = mock(ResearchRuntimeSupport.class);
        subagentRegistry = mock(SubagentRegistry.class);
        properties = mock(DeerFlowProperties.class);
        budgetMiddleware = mock(ToolOutputBudgetMiddleware.class);
        applicationContext = mock(ApplicationContext.class);
        toolPolicyService = mock(ToolPolicyService.class);
        toolRegistry = mock(ToolRegistry.class);
        subgraphRunner = mock(SubgraphRunner.class);

        when(properties.getModel()).thenReturn("test-model");
        when(properties.getWorkspaceRoot()).thenReturn(".");
        when(properties.getSubagentMaxConcurrent()).thenReturn(3);

        when(applicationContext.getBean(ToolRegistry.class)).thenReturn(toolRegistry);
        when(applicationContext.getBean(ToolPolicyService.class)).thenReturn(toolPolicyService);

        subagentRuntime = new SubagentRuntime(
                modelClient,
                researchRuntimeSupport,
                subagentRegistry,
                properties,
                budgetMiddleware
        );
        subagentRuntime.setApplicationContext(applicationContext);
        subagentRuntime.setSubgraphRunner(subgraphRunner);
        when(subgraphRunner.execute(any(SubgraphRunner.ChildRequest.class), any(SubagentExecutionHook.class)))
                .thenAnswer(invocation -> {
                    SubgraphRunner.ChildRequest request = invocation.getArgument(0);
                    return SubagentResult.success(request.parentToolCallId(), request.parentRunId(),
                            "Task complete summary", List.of(), List.of(), Map.of(), 1);
                });
    }

    @Test
    void executeReturnsErrorForUnknownSubagentType() {
        when(subagentRegistry.getConfig("unknown")).thenReturn(null);
        when(subagentRegistry.getAvailableNames()).thenReturn(List.of("general-purpose", "bash"));

        SubagentResult result = subagentRuntime.execute(
                "test desc", "test prompt", "unknown",
                "thread-1", "run-1", "parent-model", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.error()).contains("Unknown subagent type 'unknown'");
        assertThat(subagentRuntime.activeCount("run-1")).isZero();
    }

    @Test
    void executeRunsSuccessfullyAndFiltersTools() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);

        // Setup tools
        AgentTool tool1 = mock(AgentTool.class);
        when(tool1.name()).thenReturn("web_search");
        AgentTool taskTool = mock(AgentTool.class);
        when(taskTool.name()).thenReturn("task");

        when(toolRegistry.tools()).thenReturn(List.of(tool1, taskTool));

        // Allow web_search in policy, task is always excluded or disallowed inside subagent
        when(toolPolicyService.isToolAllowed(eq("web_search"), any(), any())).thenReturn(true);
        when(toolPolicyService.evaluateTool(eq("web_search"), any(), any())).thenReturn(ToolPolicyDecision.allow());

        SubagentResult result = subagentRuntime.execute(
                "Run unit tests", "Find failing tests", "general-purpose",
                "thread-123", "run-456", "qwen-plus", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.summary()).isEqualTo("Task complete summary");
        assertThat(result.parentRunId()).isEqualTo("run-456");
        assertThat(result.taskId()).startsWith("sub-");

        // Verify active tasks tracking
        assertThat(subagentRuntime.activeCount("run-456")).isEqualTo(0);

        ArgumentCaptor<SubgraphRunner.ChildRequest> requestCaptor =
                ArgumentCaptor.forClass(SubgraphRunner.ChildRequest.class);
        verify(subgraphRunner).execute(requestCaptor.capture(), any(SubagentExecutionHook.class));
        assertThat(requestCaptor.getValue().modelName()).isEqualTo("qwen-plus");
        assertThat(requestCaptor.getValue().allowedToolNames()).containsExactly("web_search");
    }

    @Test
    void executeFailsWhenNoToolsAreAvailable() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);

        // No tools
        when(toolRegistry.tools()).thenReturn(List.of());

        SubagentResult result = subagentRuntime.execute(
                "desc", "prompt", "general-purpose",
                "thread-1", "run-1", "parent-model", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).contains("No tools available for subagent");
        assertThat(subagentRuntime.activeCount("run-1")).isZero();
    }

    @Test
    void opensProviderFailureCircuitAndReleasesSlot() {
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(SubagentConfig.generalPurpose());
        when(subgraphRunner.execute(any(SubgraphRunner.ChildRequest.class), any(SubagentExecutionHook.class)))
                .thenAnswer(invocation -> {
                    SubgraphRunner.ChildRequest request = invocation.getArgument(0);
                    return SubagentResult.failed(request.parentToolCallId(), request.parentRunId(),
                            "Model call failed: 400 Bad Request", 1);
                });

        AgentTool searchTool = mock(AgentTool.class);
        when(searchTool.name()).thenReturn("web_search");
        when(toolRegistry.tools()).thenReturn(List.of(searchTool));
        when(toolPolicyService.evaluateTool(eq("web_search"), any(), any())).thenReturn(ToolPolicyDecision.allow());

        SubagentResult result = subagentRuntime.execute(
                "desc", "prompt", "general-purpose",
                "thread-1", "run-provider-failure", "parent-model", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(subagentRuntime.activeCount("run-provider-failure")).isZero();
        assertThat(subagentRuntime.hasProviderConfigurationFailure("run-provider-failure")).isTrue();
        assertThat(subagentRuntime.providerConfigurationFailureReason("run-provider-failure"))
                .contains("HTTP 400");
    }

    @Test
    void registersSubagentSearchResultsUnderParentRun() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);
        AgentTool searchTool = new AgentTool() {
            @Override
            public String name() {
                return "web_search";
            }

            @Override
            public String description() {
                return "Search";
            }

            @Override
            public boolean supports(String userMessage) {
                return true;
            }

            @Override
            public ToolResult execute(ToolRequest request) {
                return ToolResult.of(name(), "search raw");
            }
        };
        when(toolRegistry.tools()).thenReturn(List.of(searchTool));
        when(toolPolicyService.isToolAllowed(eq("web_search"), any(), any())).thenReturn(true);
        when(toolPolicyService.evaluateTool(eq("web_search"), any(), any())).thenReturn(ToolPolicyDecision.allow());
        when(researchRuntimeSupport.ingestSearchResults(eq("thread-parent"), eq("run-parent"), eq("search raw")))
                .thenReturn(new SearchIngestionResult(List.of(), "registered"));
        when(subgraphRunner.execute(any(SubgraphRunner.ChildRequest.class), any(SubagentExecutionHook.class)))
                .thenAnswer(invocation -> {
                    SubgraphRunner.ChildRequest request = invocation.getArgument(0);
                    SubagentExecutionHook hook = invocation.getArgument(1);
                    hook.afterTool(null,
                            new ExecutionToolCall("call-search", "web_search", "{}", Map.of()),
                            new ExecutionToolResult("call-search", "web_search", "{}",
                                    ExecutionToolResult.Status.SUCCESS, "search raw", "", 1, Map.of()),
                            new java.util.ArrayList<>(), new java.util.concurrent.atomic.AtomicInteger(), List.of());
                    return SubagentResult.success(request.parentToolCallId(), request.parentRunId(),
                            "Subagent summary", hook.evidenceIds(), hook.sourceIds(), Map.of(), 1);
                });

        SubagentResult result = subagentRuntime.execute(
                "desc", "prompt", "general-purpose",
                "thread-parent", "run-parent", "qwen-plus", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isTrue();
        verify(researchRuntimeSupport).ingestSearchResults("thread-parent", "run-parent", "search raw");
    }
}
