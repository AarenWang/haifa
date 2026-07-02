package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.plan.PlanGenerationResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchPlanningNode implements AsyncNodeAction {

    private final ResearchPlanner planner;
    private final ResearchPlanStore planStore;

    public ResearchPlanningNode(ResearchPlanner planner, ResearchPlanStore planStore) {
        this.planner = planner;
        this.planStore = planStore;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            String topic = state.<String>value(AgentGraphStateKeys.USER_MESSAGE).orElse("");
            ResearchOptions options = (ResearchOptions) state.data().get("researchOptions");
            if (options == null) {
                options = ResearchOptions.defaults();
            }

            PlanGenerationResult result = planner.generatePlan(threadId, runId, topic, options);
            if (result.success() && result.plan() != null) {
                ResearchPlan plan = result.plan();
                planStore.save(plan);

                Map<String, Object> planMap = new HashMap<>();
                planMap.put("planId", plan.planId());
                planMap.put("topic", plan.topic());
                planMap.put("dimensionCount", plan.dimensionCount());

                // Emit event
                AgentEvent event = AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.RESEARCH_PLAN_CREATED,
                        "Research plan created: " + plan.topic() + " (" + plan.dimensionCount() + " dimensions)",
                        Map.of("planId", plan.planId(), "dimensionCount", plan.dimensionCount())
                );
                GraphEventRegistry.publish(runId, event);

                Map<String, Object> update = new HashMap<>();
                update.put(AgentGraphStateKeys.RESEARCH_PLAN_REF, planMap);
                update.put(AgentGraphStateKeys.RESEARCH_PHASE, "planning");
                update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "create_or_load_plan", "status", "completed")));
                return update;
            } else {
                Map<String, Object> update = new HashMap<>();
                update.put(AgentGraphStateKeys.ERRORS, List.of("Planning failed: " + result.error()));
                return update;
            }
        });
    }
}
