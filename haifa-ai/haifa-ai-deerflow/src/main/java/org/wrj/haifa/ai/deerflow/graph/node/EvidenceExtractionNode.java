package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class EvidenceExtractionNode implements AsyncNodeAction {

    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final ResearchProgressTracker progressTracker;

    public EvidenceExtractionNode(ResearchRuntimeSupport researchRuntimeSupport,
                                  ResearchProgressTracker progressTracker) {
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.progressTracker = progressTracker;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            List<Object> rawEmitted = state.<List<Object>>value(AgentGraphStateKeys.EMITTED_EVIDENCE_IDS).orElse(List.of());
            List<String> emitted = rawEmitted.stream().map(String::valueOf).toList();
            List<String> newEmitted = new ArrayList<>(emitted);

            List<EvidenceItem> evidenceItems = researchRuntimeSupport.listEvidenceByRun(runId);
            for (EvidenceItem item : evidenceItems) {
                if (!newEmitted.contains(item.evidenceId())) {
                    progressTracker.recordEvidence(runId, item);
                    newEmitted.add(item.evidenceId());

                    AgentEvent event = AgentEvent.of(
                            UUID.randomUUID().toString(),
                            runId,
                            threadId,
                            AgentEventType.EVIDENCE_EXTRACTED,
                            item.claim(),
                            Map.of("evidenceId", item.evidenceId(),
                                   "sourceId", item.sourceId(),
                                   "dimension", item.dimension(),
                                   "confidence", item.confidence())
                    );
                    GraphEventRegistry.publish(runId, event);
                }
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.EMITTED_EVIDENCE_IDS, newEmitted);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "evidence");
            update.put(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, researchRuntimeSupport.listSourcesByRun(runId).size());
            update.put(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, evidenceItems.size());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "extract_evidence", "status", "completed")));
            return update;
        });
    }
}
