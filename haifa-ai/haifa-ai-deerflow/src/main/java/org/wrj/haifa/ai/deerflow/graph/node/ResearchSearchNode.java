package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.agent.ResearchOptions;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.SearchIngestionResult;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchTaskStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchSearchNode implements AsyncNodeAction {

    private final ResearchPlanStore planStore;
    private final ResearchProgressTracker progressTracker;
    private final WebSearchProviderRegistry searchRegistry;
    private final DeerFlowProperties properties;
    private final ResearchRuntimeSupport researchRuntimeSupport;

    public ResearchSearchNode(ResearchPlanStore planStore, ResearchProgressTracker progressTracker,
                              WebSearchProviderRegistry searchRegistry, DeerFlowProperties properties,
                              ResearchRuntimeSupport researchRuntimeSupport) {
        this.planStore = planStore;
        this.progressTracker = progressTracker;
        this.searchRegistry = searchRegistry;
        this.properties = properties;
        this.researchRuntimeSupport = researchRuntimeSupport;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            ResearchPlan plan = planStore.findByRunId(runId).orElse(null);
            if (plan == null) {
                return Map.of();
            }

            ResearchDimension currentDim = null;
            for (ResearchDimension dim : plan.dimensions()) {
                if (dim.status() == ResearchTaskStatus.IN_PROGRESS) {
                    currentDim = dim;
                    break;
                }
            }
            if (currentDim == null) {
                for (ResearchDimension dim : plan.dimensions()) {
                    if (dim.status() == ResearchTaskStatus.PENDING) {
                        currentDim = dim;
                        progressTracker.markDimensionStarted(runId, dim.id());
                        break;
                    }
                }
            }

            if (currentDim == null) {
                return Map.of(AgentGraphStateKeys.RESEARCH_PHASE, "search_completed");
            }

            plan = planStore.findByRunId(runId).orElse(plan);

            List<String> queries = currentDim.searchQueries();
            if (queries.isEmpty()) {
                queries = List.of(plan.topic());
            }

            String providerId = properties.getWebSearchProvider();
            WebSearchProvider provider = searchRegistry.resolve(providerId);

            String query = queries.get(0);
            int maxResults = 5;
            ResearchOptions options = (ResearchOptions) state.data().get("researchOptions");
            if (options != null) {
                maxResults = options.maxSources() / Math.max(1, plan.dimensionCount());
                if (maxResults < 3) maxResults = 3;
            }

            String searchResultRaw = provider.search(query, maxResults);
            SearchIngestionResult ingestion = researchRuntimeSupport.ingestSearchResults(threadId, runId, searchResultRaw);

            for (var reg : ingestion.registrations()) {
                AgentEvent event = AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.SOURCE_FOUND,
                        "Source found: " + reg.source().title(),
                        Map.of("sourceId", reg.source().sourceId(),
                               "url", reg.source().url(),
                               "domain", reg.source().domain(),
                               "deduplicated", reg.deduplicated())
                );
                GraphEventRegistry.publish(runId, event);
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "search");
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "search_sources", "status", "completed")));
            return update;
        });
    }
}
