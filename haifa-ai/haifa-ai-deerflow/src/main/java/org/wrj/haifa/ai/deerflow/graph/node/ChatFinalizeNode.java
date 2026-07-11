package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.loop.FinalAnswerResult;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleContext;
import org.wrj.haifa.ai.deerflow.graph.GraphChatLifecycleRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphLifecycleService;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateView;
import org.wrj.haifa.ai.deerflow.run.RunCancellationService;

@Component
public class ChatFinalizeNode implements AsyncNodeAction {

    private final GraphLifecycleService graphLifecycleService;

    public ChatFinalizeNode(GraphLifecycleService graphLifecycleService) {
        this.graphLifecycleService = graphLifecycleService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private GraphExecutionManager graphExecutionManager;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private RunCancellationService runCancellationService;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            throwIfCancelled(runId);
            String content = state.<String>value("accepted_final_answer")
                    .orElse(state.<String>value("last_assistant_content").orElse(""));
            int stepNum = state.<Integer>value("chat_steps").orElse(0);
            int totalToolCalls = AgentGraphStateView.of(state).listOfMaps(AgentGraphStateKeys.TOOL_RESULTS).size();

            String finalAnswer = content == null ? "" : content.trim();
            Map<String, Object> finalMetadata = new HashMap<>(state.<Map<String, Object>>value("accepted_final_metadata").orElse(Map.of()));

            GraphChatLifecycleContext context = GraphChatLifecycleRegistry.get(runId).orElse(null);
            if (context != null && context.observer() != null && context.runConfig() != null) {
                List<AgentEvent> observerEvents = new ArrayList<>();
                FinalAnswerResult result = context.observer().onFinalAnswerAccepted(
                        context.runConfig(), finalAnswer, observerEvents, context.eventSequence(), stepNum, totalToolCalls);
                publishObserverEvents(runId, observerEvents);
                if (result != null) {
                    if (result.finalAnswer() != null) {
                        finalAnswer = result.finalAnswer();
                    }
                    if (result.extraMetadata() != null) {
                        finalMetadata.putAll(result.extraMetadata());
                    }
                }
            }

            String mode = state.<String>value(AgentGraphStateKeys.MODE).orElse("CHAT");
            if (!"RESEARCH".equalsIgnoreCase(mode)) {
                graphLifecycleService.completeChat(runId, threadId, finalAnswer, stepNum, totalToolCalls);
            }

            Map<String, Object> completionMetadata = new HashMap<>(finalMetadata);
            completionMetadata.put("stopReason", "FINAL_ANSWER");
            completionMetadata.put("steps", stepNum);
            completionMetadata.put("totalToolCalls", totalToolCalls);
            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.RUN_COMPLETED,
                    finalAnswer,
                    completionMetadata
            ));

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.FINAL_ANSWER, finalAnswer);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "finalize", "status", "completed")));
            return update;
        }, executor);
    }

    private void throwIfCancelled(String runId) {
        if (runCancellationService != null) {
            runCancellationService.throwIfCancelled(runId);
        }
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
}