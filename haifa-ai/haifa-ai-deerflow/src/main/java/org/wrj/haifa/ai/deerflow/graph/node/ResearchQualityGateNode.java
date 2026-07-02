package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.plan.QualityGateResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchQualityGate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchQualityGateNode implements AsyncNodeAction {

    private final ResearchPlanStore planStore;
    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final ResearchQualityGate qualityGate;

    public ResearchQualityGateNode(ResearchPlanStore planStore,
                                   ResearchRuntimeSupport researchRuntimeSupport,
                                   ResearchQualityGate qualityGate) {
        this.planStore = planStore;
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.qualityGate = qualityGate;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            ResearchOptions options = (ResearchOptions) state.data().get("researchOptions");
            if (options == null) {
                options = ResearchOptions.defaults();
            }

            ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
            List<ResearchSource> sources = researchRuntimeSupport.listSourcesByRun(runId);
            List<EvidenceItem> evidenceItems = researchRuntimeSupport.listEvidenceByRun(runId);
            boolean requireCitations = Boolean.TRUE.equals(options.requireCitations());

            // Emit gate start event
            GraphEventRegistry.publish(runId, AgentEvent.of(
                    UUID.randomUUID().toString(),
                    runId,
                    threadId,
                    AgentEventType.QUALITY_GATE_STARTED,
                    "Evaluating research quality and coverage",
                    Map.of("sourceCount", sources.size(), "evidenceCount", evidenceItems.size())
            ));

            QualityGateResult readiness = qualityGate.evaluate(plan, sources, evidenceItems, requireCitations);
            boolean passed = readiness.passed();
            int currentSteps = state.<Integer>value("research_steps").orElse(0) + 1;

            AgentEvent gateResultEvent;
            if (passed) {
                gateResultEvent = AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.QUALITY_GATE_PASSED,
                        "Research quality gate passed: " + readiness.recommendation(),
                        Map.of("score", readiness.score(), "gaps", readiness.gaps())
                );
            } else {
                gateResultEvent = AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.QUALITY_GATE_FAILED,
                        "Research quality gate failed: " + String.join("; ", readiness.gaps()),
                        Map.of("score", readiness.score(), "gaps", readiness.gaps(), "recommendation", readiness.recommendation())
                );
            }
            GraphEventRegistry.publish(runId, gateResultEvent);

            Map<String, Object> update = new HashMap<>();
            update.put("quality_gate_passed", passed);
            update.put("research_steps", currentSteps);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "quality_gate");
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "quality_gate", "status", "completed")));
            return update;
        });
    }
}
