package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.graph.GraphEventRegistry;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;
import org.wrj.haifa.ai.deerflow.research.FetchProcessingResult;
import org.wrj.haifa.ai.deerflow.research.RegisteredSourceContent;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchProgressTracker;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Component
public class ResearchFetchNode implements AsyncNodeAction {

    private final ResearchRuntimeSupport researchRuntimeSupport;
    private final WebFetchProviderRegistry fetchRegistry;
    private final DeerFlowProperties properties;
    private final ResearchProgressTracker progressTracker;

    public ResearchFetchNode(ResearchRuntimeSupport researchRuntimeSupport,
                             WebFetchProviderRegistry fetchRegistry,
                             DeerFlowProperties properties,
                             ResearchProgressTracker progressTracker) {
        this.researchRuntimeSupport = researchRuntimeSupport;
        this.fetchRegistry = fetchRegistry;
        this.properties = properties;
        this.progressTracker = progressTracker;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String runId = state.<String>value(AgentGraphStateKeys.RUN_ID).orElse("");
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");

            List<ResearchSource> sources = researchRuntimeSupport.listSourcesByRun(runId);
            List<ResearchSource> unfetched = sources.stream()
                    .filter(source -> !source.fetched())
                    .toList();

            if (unfetched.isEmpty()) {
                return Map.of(
                        AgentGraphStateKeys.RESEARCH_PHASE, "fetch_skipped",
                        AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, sources.size(),
                        AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, researchRuntimeSupport.listEvidenceByRun(runId).size(),
                        AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "fetch_sources", "status", "skipped"))
                );
            }

            String providerId = properties.getWebFetchProvider();
            WebFetchProvider provider = fetchRegistry.resolve(providerId);

            // Fetch up to 3 sources per node activation to avoid rate limits/timeouts
            int limit = Math.min(3, unfetched.size());
            for (int i = 0; i < limit; i++) {
                ResearchSource source = unfetched.get(i);
                String url = source.url();

                String rawContent = "";
                Optional<RegisteredSourceContent> cached = researchRuntimeSupport.reuseFetched(url);
                if (cached.isPresent()) {
                    rawContent = cached.get().rawContent();
                } else {
                    try {
                        rawContent = provider.fetch(url);
                    } catch (Exception ex) {
                        // Skip failed fetches
                        continue;
                    }
                }

                FetchProcessingResult result = researchRuntimeSupport.ingestFetchedContent(threadId, runId, url, rawContent);
                ResearchSource storedSource = result.registration().stored().source();

                progressTracker.recordFetchedSource(runId, storedSource.sourceId());

                AgentEvent event = AgentEvent.of(
                        UUID.randomUUID().toString(),
                        runId,
                        threadId,
                        AgentEventType.SOURCE_FETCHED,
                        "Source fetched: " + storedSource.title(),
                        Map.of("sourceId", storedSource.sourceId(),
                               "url", storedSource.url(),
                               "domain", storedSource.domain(),
                               "cached", result.registration().cached(),
                               "deduplicatedByUrl", result.registration().deduplicatedByUrl(),
                               "deduplicatedByContentHash", result.registration().deduplicatedByContentHash())
                );
                GraphEventRegistry.publish(runId, event);
            }

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.RESEARCH_PHASE, "fetch");
            update.put(AgentGraphStateKeys.RESEARCH_SOURCE_COUNT, researchRuntimeSupport.listSourcesByRun(runId).size());
            update.put(AgentGraphStateKeys.RESEARCH_EVIDENCE_COUNT, researchRuntimeSupport.listEvidenceByRun(runId).size());
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "fetch_sources", "status", "completed")));
            return update;
        });
    }
}
