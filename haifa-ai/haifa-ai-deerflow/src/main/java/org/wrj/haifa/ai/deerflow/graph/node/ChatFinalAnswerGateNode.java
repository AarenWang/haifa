package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerDecision;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleContext;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;

@Component
public class ChatFinalAnswerGateNode implements AsyncNodeAction {

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            String answer = state.<String>value("last_assistant_content").orElse("");
            int step = state.<Integer>value("chat_steps").orElse(0);
            AgentGraphStateView view = AgentGraphStateView.of(state);
            int totalToolCalls = view.listOfMaps(AgentGraphStateKeys.TOOL_RESULTS).size();

            Map<String, Object> update = new HashMap<>();
            update.put("final_answer_gate_status", "ACCEPTED");
            update.put("accepted_final_answer", answer == null ? "" : answer.trim());
            update.put("accepted_final_metadata", Map.of());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "accepted")));

            GraphChatLifecycleContext context = GraphChatLifecycleRegistry.get(runId).orElse(null);
            if (context == null || context.observer() == null || context.runConfig() == null) {
                return update;
            }

            boolean maxStepsReached = context.loopConfig() != null && step >= context.loopConfig().maxSteps();
            List<AgentEvent> observerEvents = new ArrayList<>();
            List<String> history = historyFromWindow(view.messageWindow());

            if (!maxStepsReached) {
                int before = history.size();
                boolean shouldContinue = context.observer().shouldContinue(
                        context.runConfig(), answer, observerEvents, context.eventSequence(), step, totalToolCalls, history);
                publishObserverEvents(runId, observerEvents);
                if (shouldContinue) {
                    update.put("final_answer_gate_status", "CONTINUE");
                    update.put(AgentGraphStateKeys.MESSAGE_WINDOW, systemMessages(threadId, runId,
                            newHistoryEntries(history, before), Map.of("observer", "shouldContinue")));
                    update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "continue")));
                    return update;
                }
            }

            FinalAnswerDecision decision = context.observer().onFinalAnswerProposed(
                    context.runConfig(), answer, observerEvents, context.eventSequence(), step, totalToolCalls);
            publishObserverEvents(runId, observerEvents);
            if (decision != null && !decision.accepted() && !maxStepsReached) {
                update.put("final_answer_gate_status", "CONTINUE");
                update.put(AgentGraphStateKeys.MESSAGE_WINDOW, List.of(systemMessage(threadId, runId,
                        decision.retryInstruction(), Map.of("observer", "onFinalAnswerProposed", "finalAnswerRejected", true))));
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "final_answer_gate", "status", "rejected")));
                return update;
            }

            if (decision != null && decision.accepted()) {
                update.put("accepted_final_answer", decision.answer());
                update.put("accepted_final_metadata", decision.metadata());
            }
            if (maxStepsReached) {
                context.observer().onMaxStepsReached(context.runConfig(), answer,
                        observerEvents, context.eventSequence(), step, totalToolCalls);
                publishObserverEvents(runId, observerEvents);
            }
            return update;
        }, executor);
    }

    private static void publishObserverEvents(String runId, List<AgentEvent> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        for (AgentEvent event : events) {
            GraphEventRegistry.publish(runId, event);
        }
        events.clear();
    }

    private static List<String> historyFromWindow(List<Map<String, Object>> window) {
        List<String> history = new ArrayList<>();
        if (window == null) {
            return history;
        }
        for (Map<String, Object> message : window) {
            String role = stringValue(message.get("role"));
            String content = stringValue(message.get("content"));
            if (content.isBlank()) {
                continue;
            }
            history.add(roleLabel(role) + ": " + content);
        }
        return history;
    }

    private static List<String> newHistoryEntries(List<String> history, int before) {
        if (history == null || before >= history.size()) {
            return List.of();
        }
        return history.subList(Math.max(0, before), history.size());
    }

    private static List<Map<String, Object>> systemMessages(String threadId, String runId,
            List<String> entries, Map<String, Object> metadata) {
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }
        return entries.stream()
                .map(entry -> systemMessage(threadId, runId, stripRolePrefix(entry), metadata))
                .toList();
    }

    private static Map<String, Object> systemMessage(String threadId, String runId, String content,
            Map<String, Object> metadata) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("messageId", UUID.randomUUID().toString());
        message.put("threadId", threadId);
        message.put("runId", runId);
        message.put("role", ModelMessage.Role.SYSTEM.name());
        message.put("content", content == null ? "" : content);
        message.put("metadata", metadata == null ? Map.of() : metadata);
        message.put("createdAt", java.time.Instant.now().toString());
        return message;
    }

    private static String roleLabel(String role) {
        if ("ASSISTANT".equalsIgnoreCase(role)) return "Assistant";
        if ("TOOL".equalsIgnoreCase(role)) return "Tool result";
        if ("SYSTEM".equalsIgnoreCase(role)) return "System";
        return "User";
    }

    private static String stripRolePrefix(String value) {
        if (value == null) return "";
        if (value.startsWith("System: ")) return value.substring("System: ".length());
        return value;
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}