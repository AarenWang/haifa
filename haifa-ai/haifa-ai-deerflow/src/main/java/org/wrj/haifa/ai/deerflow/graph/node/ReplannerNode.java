package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;

@Component
public class ReplannerNode implements AsyncNodeAction {

    private final ResearchPlanStore planStore;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public ReplannerNode(ResearchPlanStore planStore) {
        this.planStore = planStore;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null
                ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
            if (plan == null) {
                return Map.of(AgentGraphStateKeys.MODEL_STEPS,
                        List.of(Map.of("node", "replan", "status", "skipped")));
            }

            List<String> gaps = normalizeGaps(state.data().get(AgentGraphStateKeys.RESEARCH_GAPS));
            int replanCount = state.<Integer>value(AgentGraphStateKeys.REPLAN_COUNT).orElse(0) + 1;
            if (!gaps.isEmpty()) {
                List<ResearchDimension> dimensions = new ArrayList<>(plan.dimensions());
                String gapText = String.join("; ", gaps);
                dimensions.add(new ResearchDimension(
                        "replan-" + replanCount,
                        "Replan gap coverage " + replanCount,
                        gapText,
                        ResearchTaskStatus.PENDING,
                        List.of(plan.topic() + " " + gapText),
                        2,
                        0,
                        0,
                        List.of()
                ));
                planStore.save(plan.withDimensions(dimensions).withStatus("REPLANNED"));
            }

            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.RESEARCH_STEP_COMPLETED,
                    "Research graph replanned from quality gaps",
                    Map.of("replanCount", replanCount, "gaps", gaps)
            ));

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.REPLAN_COUNT, replanCount);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "replan");
            update.put(AgentGraphStateKeys.MODEL_STEPS,
                    List.of(Map.of("node", "replan", "status", "completed", "gapCount", gaps.size())));
            return update;
        }, executor);
    }

    private static List<String> normalizeGaps(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).filter(text -> !text.isBlank()).toList();
        }
        if (value == null) {
            return List.of();
        }
        String text = String.valueOf(value);
        return text.isBlank() ? List.of() : List.of(text);
    }
}
