package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphLifecycleService;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.thread.MessageRole;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatFinalizeNode implements AsyncNodeAction {

    private final GraphLifecycleService graphLifecycleService;

    public ChatFinalizeNode(GraphLifecycleService graphLifecycleService) {
        this.graphLifecycleService = graphLifecycleService;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private GraphExecutionManager graphExecutionManager;

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            String content = state.<String>value("last_assistant_content").orElse("");
            int stepNum = state.<Integer>value("chat_steps").orElse(0);

            String finalAnswer = content == null ? "" : content.trim();

            graphLifecycleService.completeChat(runId, threadId, finalAnswer, stepNum, 0);

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.RUN_COMPLETED,
                    finalAnswer,
                    Map.of("stopReason", "FINAL_ANSWER", "steps", stepNum)
            ));

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.FINAL_ANSWER, finalAnswer);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "finalize", "status", "completed")));
            return update;
        }, executor);
    }
}
