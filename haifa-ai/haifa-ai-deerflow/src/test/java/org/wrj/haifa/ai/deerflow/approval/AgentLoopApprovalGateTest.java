package org.wrj.haifa.ai.deerflow.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.agent.loop.AgentLoop;
import org.wrj.haifa.ai.deerflow.agent.loop.LoopConfig;
import org.wrj.haifa.ai.deerflow.model.AgentModelClient;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.model.ModelResponse;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore;
import org.wrj.haifa.ai.deerflow.persistence.store.ToolCallStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyDecision;
import org.wrj.haifa.ai.deerflow.tool.ToolPolicyService;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;
import reactor.core.publisher.Mono;

class AgentLoopApprovalGateTest {

    private AgentModelClient modelClient;
    private ToolRegistry toolRegistry;
    private ModelStepStore modelStepStore;
    private ToolCallStore toolCallStore;
    private AgentLoopRunStore agentLoopRunStore;
    private ApprovalPolicyService approvalPolicyService;
    private ApprovalStore approvalStore;
    private AgentLoop agentLoop;

    @BeforeEach
    void setUp() {
        modelClient = mock(AgentModelClient.class);
        toolRegistry = mock(ToolRegistry.class);
        modelStepStore = mock(ModelStepStore.class);
        toolCallStore = mock(ToolCallStore.class);
        agentLoopRunStore = mock(AgentLoopRunStore.class);
        approvalPolicyService = mock(ApprovalPolicyService.class);
        approvalStore = mock(ApprovalStore.class);

        agentLoop = new AgentLoop(
                modelClient, toolRegistry, modelStepStore, toolCallStore, agentLoopRunStore,
                null, null, null, approvalPolicyService, approvalStore
        );
    }

    @Test
    void testLoopSuspendsOnRequireApproval() {
        LoopConfig config = new LoopConfig(5, 5, 300000L, ResearchOptions.defaults());
        AgentRunConfig runConfig = new AgentRunConfig("thread-1", "run-1", "gpt-4", false, false, 5, java.nio.file.Path.of("."), RunMode.CHAT, ResearchOptions.defaults(), Map.of());
        ToolPolicyService toolPolicy = mock(ToolPolicyService.class);
        when(toolPolicy.isToolAllowed(any(), any(), any())).thenReturn(true);
        when(toolPolicy.evaluateTool(any(), any(), any())).thenReturn(ToolPolicyDecision.allow());

        // Mock tool
        AgentTool mockTool = mock(AgentTool.class);
        when(mockTool.name()).thenReturn("run_script");
        when(toolRegistry.tools()).thenReturn(List.of(mockTool));

        // Mock model response to request run_script
        ModelResponse modelResponse = new ModelResponse(
                "Assistant thinking...",
                List.of(new ModelToolCall("call-1", "run_script", "{\"script\":\"rm -rf\"}"))
        );
        when(modelClient.generate(any(ModelPrompt.class))).thenReturn(Mono.just(modelResponse));

        // Mock approval evaluation to REQUIRE_APPROVAL
        ApprovalPolicyDecision decision = new ApprovalPolicyDecision(
                ApprovalPolicyDecisionType.REQUIRE_APPROVAL,
                RiskLevel.MEDIUM,
                "needs approval",
                "risk-key",
                "preview script",
                Map.of()
        );
        when(approvalPolicyService.evaluate(any(), any(), any())).thenReturn(decision);
        when(approvalPolicyService.hashArgs(any(), any())).thenReturn("hash-123");

        // Mock approval store creating the request
        ApprovalRequestRecord record = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", "{\"script\":\"rm -rf\"}",
                "hash-123", "risk-key", RiskLevel.MEDIUM, "needs approval", "", "preview script",
                Map.of(), ApprovalStatus.PENDING, Instant.now(), Instant.now().plusSeconds(120),
                null, null, null, null
        );
        when(approvalStore.create(any())).thenReturn(record);

        // Execute run
        List<AgentEvent> events = agentLoop.run(
                config, runConfig, "system prompt", "user message", new AtomicInteger(1),
                toolPolicy, List.of(), List.of()
        ).collectList().block();

        // Verify suspended event, approval required, and store interactions
        assertThat(events).anySatisfy(evt -> {
            assertThat(evt.type()).isEqualTo(AgentEventType.APPROVAL_REQUIRED);
            assertThat(evt.metadata().get("approvalId")).isEqualTo("app-1");
        });
        assertThat(events).anySatisfy(evt -> {
            assertThat(evt.type()).isEqualTo(AgentEventType.RUN_SUSPENDED);
        });

        verify(agentLoopRunStore).markSuspended(eq("run-1"), eq("APPROVAL_REQUIRED"));
    }
}
