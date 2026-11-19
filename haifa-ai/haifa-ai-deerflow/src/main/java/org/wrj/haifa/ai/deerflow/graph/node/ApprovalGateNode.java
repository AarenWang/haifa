package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalCreateRequest;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyDecision;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyDecisionType;
import org.wrj.haifa.ai.deerflow.approval.ApprovalPolicyService;
import org.wrj.haifa.ai.deerflow.approval.ApprovalRequestRecord;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStatus;
import org.wrj.haifa.ai.deerflow.approval.ApprovalStore;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRegistry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ApprovalGateNode implements AsyncNodeAction {

    private final ToolRegistry toolRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired(required = false)
    private ApprovalPolicyService approvalPolicyService;

    @Autowired(required = false)
    private ApprovalStore approvalStore;

    @Autowired(required = false)
    private AgentLoopRunStore agentLoopRunStore;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public ApprovalGateNode(ToolRegistry toolRegistry) {
        this.toolRegistry = toolRegistry;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            AgentGraphStateView view = AgentGraphStateView.of(state);
            String runId = view.runId();
            String threadId = view.threadId();
            List<Map<String, Object>> pending = view.listOfMaps(AgentGraphStateKeys.PENDING_TOOL_CALLS);

            if (pending == null || pending.isEmpty() || approvalPolicyService == null || approvalStore == null) {
                return Map.of("approval_gate_status", "PROCEED");
            }

            // Build AgentRunConfig to pass to policy evaluate
            String modelName = view.modelName();
            if (modelName == null || modelName.isBlank()) {
                modelName = state.<String>value(AgentGraphStateKeys.MODEL_NAME).orElse("");
            }
            org.wrj.haifa.ai.deerflow.agent.AgentRunConfig runConfig = new org.wrj.haifa.ai.deerflow.agent.AgentRunConfig(
                    threadId,
                    runId,
                    modelName,
                    false,
                    false,
                    10,
                    java.nio.file.Path.of(""),
                    view.mode(),
                    org.wrj.haifa.ai.deerflow.agent.ResearchOptions.defaults(),
                    view.map(AgentGraphStateKeys.REQUEST_METADATA)
            );

            // Check if resumed with an approvalId
            String resumeApprovalId = null;
            var requestMetadata = view.map(AgentGraphStateKeys.REQUEST_METADATA);
            if (requestMetadata != null) {
                resumeApprovalId = (String) requestMetadata.get("approvalId");
            }

            for (Map<String, Object> call : pending) {
                String callId = (String) call.get("id");
                String name = (String) call.get("name");
                String arguments = (String) call.get("arguments");

                AgentTool tool = toolRegistry.tools().stream()
                        .filter(t -> t.name().equals(name))
                        .findFirst()
                        .orElse(null);

                if (tool != null) {
                    ModelToolCall modelToolCall = new ModelToolCall(callId, name, arguments);
                    ApprovalPolicyDecision decision = approvalPolicyService.evaluate(modelToolCall, tool, runConfig);

                    if (decision.type() == ApprovalPolicyDecisionType.DENY) {
                        String denMsg = "POLICY_BLOCKED: " + decision.reason() + ". This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";

                        List<Map<String, Object>> toolMessages = new ArrayList<>(view.messageWindow());
                        Map<String, Object> toolMsg = new LinkedHashMap<>();
                        toolMsg.put("messageId", UUID.randomUUID().toString());
                        toolMsg.put("threadId", threadId);
                        toolMsg.put("runId", runId);
                        toolMsg.put("role", "TOOL");
                        toolMsg.put("content", denMsg);
                        toolMsg.put("name", name);
                        toolMsg.put("toolCallId", callId);
                        toolMsg.put("metadata", Map.of("denied", true, "reason", decision.reason() == null ? "" : decision.reason()));
                        toolMsg.put("createdAt", java.time.Instant.now().toString());
                        toolMessages.add(toolMsg);

                        List<Map<String, Object>> toolResultsList = new ArrayList<>(view.toolResults());
                        Map<String, Object> resMap = new LinkedHashMap<>();
                        resMap.put("toolName", name);
                        resMap.put("result", denMsg);
                        resMap.put("metadata", Map.of("denied", true, "reason", decision.reason() == null ? "" : decision.reason()));
                        toolResultsList.add(resMap);

                        Map<String, Object> update = new HashMap<>();
                        update.put("approval_gate_status", "DENIED");
                        update.put(AgentGraphStateKeys.MESSAGE_WINDOW, toolMessages);
                        update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
                        update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
                        return update;
                    }

                    if (decision.type() == ApprovalPolicyDecisionType.REQUIRE_APPROVAL) {
                        // Check if this was already approved or denied
                        if (resumeApprovalId != null) {
                            var appRecordOpt = approvalStore.find(resumeApprovalId);
                            if (appRecordOpt.isPresent()) {
                                var appRecord = appRecordOpt.get();
                                if (appRecord.status() == ApprovalStatus.APPROVED) {
                                    continue; // Approved, check other tools
                                } else if (appRecord.status() == ApprovalStatus.DENIED) {
                                    String denMsg = "POLICY_BLOCKED: User denied approval request: " + appRecord.reason() + ". This action was not executed. Do not retry this action, rephrase it, switch tools, or bypass the user's decision.";

                                    List<Map<String, Object>> toolMessages = new ArrayList<>(view.messageWindow());
                                    Map<String, Object> toolMsg = new LinkedHashMap<>();
                                    toolMsg.put("messageId", UUID.randomUUID().toString());
                                    toolMsg.put("threadId", threadId);
                                    toolMsg.put("runId", runId);
                                    toolMsg.put("role", "TOOL");
                                    toolMsg.put("content", denMsg);
                                    toolMsg.put("name", name);
                                    toolMsg.put("toolCallId", callId);
                                    toolMsg.put("metadata", Map.of("denied", true, "reason", "User denied: " + appRecord.reason()));
                                    toolMsg.put("createdAt", java.time.Instant.now().toString());
                                    toolMessages.add(toolMsg);

                                    List<Map<String, Object>> toolResultsList = new ArrayList<>(view.toolResults());
                                    Map<String, Object> resMap = new LinkedHashMap<>();
                                    resMap.put("toolName", name);
                                    resMap.put("result", denMsg);
                                    resMap.put("metadata", Map.of("denied", true, "reason", "User denied: " + appRecord.reason()));
                                    toolResultsList.add(resMap);

                                    Map<String, Object> update = new HashMap<>();
                                    update.put("approval_gate_status", "DENIED");
                                    update.put(AgentGraphStateKeys.MESSAGE_WINDOW, toolMessages);
                                    update.put(AgentGraphStateKeys.TOOL_RESULTS, toolResultsList);
                                    update.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
                                    return update;
                                }
                            }
                        }

                        // Otherwise, create approval request and suspend
                        String calculatedHash = approvalPolicyService.hashArgs(name, arguments);
                        ApprovalCreateRequest createReq = new ApprovalCreateRequest(
                                runId,
                                threadId,
                                callId,
                                name,
                                arguments,
                                calculatedHash,
                                decision.riskKey(),
                                decision.riskLevel(),
                                decision.reason(),
                                "",
                                decision.preview(),
                                decision.metadata()
                        );

                        try {
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(arguments);
                            if (node.has("purpose")) {
                                createReq = new ApprovalCreateRequest(
                                        runId,
                                        threadId,
                                        callId,
                                        name,
                                        arguments,
                                        calculatedHash,
                                        decision.riskKey(),
                                        decision.riskLevel(),
                                        decision.reason(),
                                        node.get("purpose").asText(),
                                        decision.preview(),
                                        decision.metadata()
                                );
                            }
                        } catch (Exception ignored) {}

                        ApprovalRequestRecord record = approvalStore.create(createReq);

                        // Emit APPROVAL_REQUIRED and RUN_SUSPENDED
                        GraphEventRegistry.publish(runId, AgentEvent.of(
                                UUID.randomUUID().toString(),
                                runId,
                                threadId,
                                AgentEventType.APPROVAL_REQUIRED,
                                "Approval required: " + record.reason(),
                                Map.of(
                                        "approvalId", record.approvalId(),
                                        "toolCallId", record.toolCallId(),
                                        "toolName", record.toolName(),
                                        "riskLevel", record.riskLevel().name(),
                                        "reason", record.reason(),
                                        "purpose", record.purpose(),
                                        "preview", record.preview(),
                                        "argsHash", record.argsHash(),
                                        "expiresAt", record.expiresAt().toString()
                                )
                        ));

                        GraphEventRegistry.publish(runId, AgentEvent.of(
                                UUID.randomUUID().toString(),
                                runId,
                                threadId,
                                AgentEventType.RUN_SUSPENDED,
                                "Run suspended waiting for human approval.",
                                Map.of(
                                        "suspendReason", "APPROVAL_REQUIRED",
                                        "approvalId", record.approvalId(),
                                        "resumeThreadId", threadId,
                                        "resumeRunId", runId
                                )
                        ));

                        if (agentLoopRunStore != null) {
                            agentLoopRunStore.markSuspended(runId, "APPROVAL_REQUIRED");
                        }

                        Map<String, Object> update = new HashMap<>();
                        update.put("approval_gate_status", "SUSPEND");
                        update.put("suspend_reason", "APPROVAL_REQUIRED");
                        update.put("approval_id", record.approvalId());
                        return update;
                    }
                }
            }

            return Map.of("approval_gate_status", "PROCEED");
        }, executor);
    }
}
