package org.wrj.haifa.ai.deerflow.subagent;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.middleware.ToolOutputBudgetMiddleware;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
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

        when(properties.getModel()).thenReturn("test-model");
        when(properties.getWorkspaceRoot()).thenReturn(".");

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
    }

    @Test
    void executeReturnsErrorForUnknownSubagentType() {
        when(subagentRegistry.getConfig("unknown")).thenReturn(null);
        when(subagentRegistry.getAvailableNames()).thenReturn(List.of("general-purpose", "bash"));

        SubagentResult result = subagentRuntime.execute(
                "test desc", "test prompt", "unknown",
                "thread-1", "run-1", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.status()).isEqualTo("FAILED");
        assertThat(result.error()).contains("Unknown subagent type 'unknown'");
    }

    @Test
    void executeRunsSuccessfullyAndFiltersTools() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);

        // Mock model response
        when(modelClient.generate(any(org.wrj.haifa.ai.deerflow.model.ModelPrompt.class)))
                .thenReturn(Mono.just(new ModelResponse("Task complete summary")));

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
                "thread-123", "run-456", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.status()).isEqualTo("COMPLETED");
        assertThat(result.summary()).isEqualTo("Task complete summary");
        assertThat(result.parentRunId()).isEqualTo("run-456");
        assertThat(result.taskId()).startsWith("sub-");

        // Verify active tasks tracking
        assertThat(subagentRuntime.activeCount("run-456")).isEqualTo(0);
    }

    @Test
    void executeFailsWhenNoToolsAreAvailable() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);

        // No tools
        when(toolRegistry.tools()).thenReturn(List.of());

        SubagentResult result = subagentRuntime.execute(
                "desc", "prompt", "general-purpose",
                "thread-1", "run-1", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.error()).contains("No tools available for subagent");
    }

    @Test
    void registersSubagentSearchResultsUnderParentRun() {
        SubagentConfig config = SubagentConfig.generalPurpose();
        when(subagentRegistry.getConfig("general-purpose")).thenReturn(config);
        when(modelClient.generate(any(org.wrj.haifa.ai.deerflow.model.ModelPrompt.class)))
                .thenReturn(Mono.just(new ModelResponse("", List.of(
                        new ModelToolCall("call-search", "web_search", "{\"query\":\"parent evidence\"}")
                ))))
                .thenReturn(Mono.just(new ModelResponse("Subagent summary")));

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

        SubagentResult result = subagentRuntime.execute(
                "desc", "prompt", "general-purpose",
                "thread-parent", "run-parent", null, null, null, null,
                RunMode.RESEARCH, List.of()
        );

        assertThat(result.isSuccess()).isTrue();
        verify(researchRuntimeSupport).ingestSearchResults("thread-parent", "run-parent", "search raw");
    }
}
