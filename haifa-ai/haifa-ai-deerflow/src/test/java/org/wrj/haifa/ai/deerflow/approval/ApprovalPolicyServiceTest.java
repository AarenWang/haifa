package org.wrj.haifa.ai.deerflow.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;

class ApprovalPolicyServiceTest {

    private ApprovalPolicyService approvalPolicyService;
    private DeerFlowProperties properties;
    private ApprovalStore approvalStore;
    private org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy commandPolicy;

    @BeforeEach
    void setUp() {
        properties = new DeerFlowProperties();
        DeerFlowProperties.Approval approvalProps = new DeerFlowProperties.Approval();
        approvalProps.setEnabled(true);
        properties.setApproval(approvalProps);

        approvalStore = mock(ApprovalStore.class);
        when(approvalStore.findByThreadId(any())).thenReturn(List.of());

        commandPolicy = mock(org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy.class);
        when(commandPolicy.evaluateScriptBody(any())).thenReturn(
                new org.wrj.haifa.ai.deerflow.sandbox.CommandPolicy.Decision(true, "ok"));

        approvalPolicyService = new ApprovalPolicyService(properties, approvalStore, commandPolicy);
    }

    @Test
    void testLowRiskNonMonitoredTool() {
        ModelToolCall toolCall = new ModelToolCall("call-1", "read_file", "{\"path\": \"test.txt\"}");
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("read_file");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);
        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.ALLOW);
        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.LOW);
    }

    @Test
    void testRequireApprovalForMonitoredTool() {
        ModelToolCall toolCall = new ModelToolCall("call-2", "run_script", "{\"script\": \"echo hello\", \"backend\": \"local\"}");
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("run_script");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);
        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.REQUIRE_APPROVAL);
        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.HIGH);
    }

    @Test
    void testDenyBlacklistedPatterns() {
        ModelToolCall toolCall = new ModelToolCall("call-3", "run_script", "{\"script\": \"sudo rm -rf /\", \"backend\": \"local\"}");
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("run_script");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);
        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.DENY);
        assertThat(decision.riskLevel()).isEqualTo(RiskLevel.BLOCKED);
        assertThat(decision.reason()).contains("hardline refuse pattern");
    }

    @Test
    void askClarificationArgumentsAreNotScannedAsCommands() {
        ModelToolCall toolCall = new ModelToolCall("call-clarify", "ask_clarification", """
                {"questions":[{"id":"delivery_format","title":"Output format","prompt":"Which format?"}]}
                """);
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("ask_clarification");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);

        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.ALLOW);
    }

    @Test
    void testHashArgs() {
        String hash1 = approvalPolicyService.hashArgs("run_script", "{\"script\":\"echo\"}");
        String hash2 = approvalPolicyService.hashArgs("run_script", "{\"script\":\"echo\"}");
        String hash3 = approvalPolicyService.hashArgs("run_script", "{\"script\":\"echo hello\"}");

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).isNotEqualTo(hash3);
    }

    @Test
    void testSessionApprovalDoesNotAllowChangedArguments() {
        String approvedArgs = "{\"language\":\"python\",\"code\":\"print('cpu')\"}";
        String changedArgs = "{\"language\":\"python\",\"code\":\"print('different')\"}";
        String riskKey = approvalPolicyService.generateRiskKey("run_script", approvedArgs);
        ApprovalRequestRecord approved = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", approvedArgs,
                approvalPolicyService.hashArgs("run_script", approvedArgs),
                riskKey, RiskLevel.HIGH, "reason", "", "preview",
                Map.of(), ApprovalStatus.EXECUTED, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                "user", java.time.Instant.now(), ApprovalDecisionType.APPROVE_SESSION, "ok"
        );
        when(approvalStore.findByThreadId("thread-1")).thenReturn(List.of(approved));

        ModelToolCall toolCall = new ModelToolCall("call-2", "run_script", changedArgs);
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("run_script");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);

        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.REQUIRE_APPROVAL);
    }

    @Test
    void testSessionApprovalAllowsExecutedRecordWithSameArguments() {
        String args = "{\"language\":\"python\",\"code\":\"print('cpu')\"}";
        String riskKey = approvalPolicyService.generateRiskKey("run_script", args);
        ApprovalRequestRecord approved = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", args,
                approvalPolicyService.hashArgs("run_script", args),
                riskKey, RiskLevel.HIGH, "reason", "", "preview",
                Map.of(), ApprovalStatus.EXECUTED, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                "user", java.time.Instant.now(), ApprovalDecisionType.APPROVE_SESSION, "ok"
        );
        when(approvalStore.findByThreadId("thread-1")).thenReturn(List.of(approved));

        ModelToolCall toolCall = new ModelToolCall("call-2", "run_script", args);
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("run_script");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-1");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);

        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.ALLOW);
    }

    @Test
    void testAlwaysApprovalAllowsGlobally() {
        properties.getApproval().setAllowAlwaysApproval(true);
        String args = "{\"language\":\"python\",\"code\":\"print('cpu')\"}";
        String riskKey = approvalPolicyService.generateRiskKey("run_script", args);
        ApprovalRequestRecord approved = new ApprovalRequestRecord(
                "app-1", "run-1", "thread-1", "call-1", "run_script", args,
                approvalPolicyService.hashArgs("run_script", args),
                riskKey, RiskLevel.HIGH, "reason", "", "preview",
                Map.of(), ApprovalStatus.APPROVED, java.time.Instant.now(), java.time.Instant.now().plusSeconds(120),
                "user", java.time.Instant.now(), ApprovalDecisionType.APPROVE_ALWAYS, "ok"
        );
        when(approvalStore.findAlwaysApprovals()).thenReturn(List.of(approved));

        ModelToolCall toolCall = new ModelToolCall("call-2", "run_script", args);
        AgentTool tool = mock(AgentTool.class);
        when(tool.name()).thenReturn("run_script");
        AgentRunConfig runConfig = mock(AgentRunConfig.class);
        when(runConfig.threadId()).thenReturn("thread-different");

        ApprovalPolicyDecision decision = approvalPolicyService.evaluate(toolCall, tool, runConfig);

        assertThat(decision.type()).isEqualTo(ApprovalPolicyDecisionType.ALLOW);
        assertThat(decision.reason()).contains("Always approved action matched globally");
    }
}
