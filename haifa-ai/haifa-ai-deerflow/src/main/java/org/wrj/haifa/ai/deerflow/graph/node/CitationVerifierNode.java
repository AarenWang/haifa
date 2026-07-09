package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.GraphExecutionManager;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;

@Component
public class CitationVerifierNode implements AsyncNodeAction {

    private final ResearchRuntimeSupport researchRuntimeSupport;

    @Autowired
    private GraphExecutionManager graphExecutionManager;

    public CitationVerifierNode(ResearchRuntimeSupport researchRuntimeSupport) {
        this.researchRuntimeSupport = researchRuntimeSupport;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        java.util.concurrent.Executor executor = graphExecutionManager != null
                ? graphExecutionManager.getExecutor() : GraphExecutionManager.fallbackExecutor();
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            List<ResearchSource> sources = researchRuntimeSupport.listSourcesByRun(runId);
            List<EvidenceItem> evidence = researchRuntimeSupport.listEvidenceByRun(runId);
            List<String> sourceIds = sources.stream().map(ResearchSource::sourceId).toList();
            List<String> orphanEvidenceIds = evidence.stream()
                    .filter(item -> !sourceIds.contains(item.sourceId()))
                    .map(EvidenceItem::evidenceId)
                    .toList();

            Map<String, Object> verification = new HashMap<>();
            verification.put("verified", orphanEvidenceIds.isEmpty());
            verification.put("sourceCount", sources.size());
            verification.put("evidenceCount", evidence.size());
            verification.put("orphanEvidenceIds", orphanEvidenceIds);
            verification.put("traceableEvidenceCount", evidence.size() - orphanEvidenceIds.size());

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.CITATION_VERIFICATION, verification);
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "citation_verification");
            update.put(AgentGraphStateKeys.MODEL_STEPS,
                    List.of(Map.of("node", "verify_citations", "status", "completed",
                            "verified", orphanEvidenceIds.isEmpty())));
            return update;
        }, executor);
    }
}
