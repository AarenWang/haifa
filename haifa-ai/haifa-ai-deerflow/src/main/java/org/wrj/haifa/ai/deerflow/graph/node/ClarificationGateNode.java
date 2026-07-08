package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentLoopRunStore;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ClarificationGateNode implements AsyncNodeAction {

    @Autowired(required = false)
    private AgentLoopRunStore agentLoopRunStore;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            AgentGraphStateView view = AgentGraphStateView.of(state);
            String runId = view.runId();
            String threadId = view.threadId();

            Map<String, Object> clarMeta = state.<Map<String, Object>>value("clarification_metadata").orElse(null);
            if (clarMeta == null || clarMeta.isEmpty()) {
                return Map.of("clarification_gate_status", "PROCEED");
            }

            String question = (String) clarMeta.get("question");
            String clarificationId = (String) clarMeta.get("clarificationId");
            String type = (String) clarMeta.getOrDefault("clarificationType", "missing_info");
            Object options = clarMeta.getOrDefault("options", java.util.List.of());
            Object questions = clarMeta.getOrDefault("questions", java.util.List.of());

            Map<String, Object> eventMetadata = new HashMap<>(clarMeta);
            eventMetadata.put("resumeThreadId", threadId);
            eventMetadata.put("resumeRunId", runId);

            // Emit CLARIFICATION_REQUIRED and RUN_SUSPENDED
            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.CLARIFICATION_REQUIRED,
                    "Clarification required: " + question,
                    eventMetadata
            ));

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.RUN_SUSPENDED,
                    "Run suspended waiting for user clarification.",
                    eventMetadata
            ));

            if (agentLoopRunStore != null) {
                agentLoopRunStore.markSuspended(runId, "CLARIFICATION_REQUIRED");
            }

            Map<String, Object> update = new HashMap<>();
            update.put("clarification_gate_status", "SUSPEND");
            update.put("suspend_reason", "CLARIFICATION_REQUIRED");
            update.put("clarification_id", clarificationId);
            return update;
        }, executor);
    }
}
