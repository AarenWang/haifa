package org.wrj.haifa.ai.deerflow.middleware;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.RunMode;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactRecord;
import org.wrj.haifa.ai.deerflow.artifact.ArtifactService;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.EvidenceStore;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.SourceRegistry;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchDimension;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlan;
import org.wrj.haifa.ai.deerflow.research.plan.ResearchPlanStore;
import reactor.core.publisher.Mono;

@Component
@MiddlewareOrder(20)
@MiddlewareLifecycle(MiddlewarePhase.MODEL_INPUT)
public class ThreadMemoryMiddleware implements AgentMiddleware {

    private final ResearchPlanStore planStore;
    private final SourceRegistry sourceRegistry;
    private final EvidenceStore evidenceStore;
    private final ArtifactService artifactService;

    public ThreadMemoryMiddleware(
            ResearchPlanStore planStore,
            SourceRegistry sourceRegistry,
            EvidenceStore evidenceStore,
            ArtifactService artifactService) {
        this.planStore = planStore;
        this.sourceRegistry = sourceRegistry;
        this.evidenceStore = evidenceStore;
        this.artifactService = artifactService;
    }

    @Override
    public Mono<ModelPrompt> apply(AgentRuntimeContext context, MiddlewareChain next) {
        if (context.config().mode() != RunMode.RESEARCH) {
            return next.next(context);
        }

        String threadId = context.config().threadId();

        StringBuilder memoryBuilder = new StringBuilder();
        memoryBuilder.append("\n<thread_research_memory>\n");

        // 1. Previous research plan
        if (planStore != null) {
            List<ResearchPlan> plans = planStore.findByThreadId(threadId);
            if (plans != null && !plans.isEmpty()) {
                ResearchPlan plan = plans.get(plans.size() - 1);
                memoryBuilder.append("Previous Research Plan:\n");
                memoryBuilder.append("- Topic: ").append(plan.topic()).append("\n");
                for (ResearchDimension dim : plan.dimensions()) {
                    memoryBuilder.append("  * [").append(dim.status().name()).append("] ").append(dim.title())
                                 .append(" (Expected sources: ").append(dim.expectedSourceCount())
                                 .append(", Fetched: ").append(dim.actualSourceCount()).append(")\n");
                }
            }
        }

        // 2. Source registry
        if (sourceRegistry != null) {
            List<ResearchSource> sources = sourceRegistry.listByThread(threadId);
            if (!sources.isEmpty()) {
                memoryBuilder.append("Registered Sources (can be reused, do not fetch again):\n");
                int limit = 10;
                for (int i = 0; i < Math.min(sources.size(), limit); i++) {
                    ResearchSource src = sources.get(i);
                    memoryBuilder.append("  * [").append(src.sourceId()).append("] ").append(src.title())
                                 .append(" (URL: ").append(src.url()).append(", domain: ").append(src.domain()).append(")\n");
                }
                if (sources.size() > limit) {
                    memoryBuilder.append("  * ... and ").append(sources.size() - limit).append(" more sources.\n");
                }
            }
        }

        // 3. Evidence store summary
        if (evidenceStore != null) {
            List<EvidenceItem> evidence = evidenceStore.listByThread(threadId);
            if (!evidence.isEmpty()) {
                memoryBuilder.append("Evidence Store Summary:\n");
                int limit = 10;
                for (int i = 0; i < Math.min(evidence.size(), limit); i++) {
                    EvidenceItem item = evidence.get(i);
                    memoryBuilder.append("  * [").append(item.evidenceId()).append("] (Source: ").append(item.sourceId()).append(", Dimension: ").append(item.dimension()).append("): ")
                                 .append(item.claim()).append("\n");
                }
                if (evidence.size() > limit) {
                    memoryBuilder.append("  * ... and ").append(evidence.size() - limit).append(" more evidence items.\n");
                }
            }
        }

        // 4. Artifact list
        if (artifactService != null) {
            List<ArtifactRecord> artifacts = artifactService.list(threadId, null);
            if (!artifacts.isEmpty()) {
                memoryBuilder.append("Generated Artifacts:\n");
                int limit = 5;
                for (int i = 0; i < Math.min(artifacts.size(), limit); i++) {
                    ArtifactRecord art = artifacts.get(i);
                    memoryBuilder.append("  * ").append(art.filename()).append(" (ID: ").append(art.artifactId()).append(", Size: ").append(art.size()).append(" bytes)\n");
                }
                if (artifacts.size() > limit) {
                    memoryBuilder.append("  * ... and ").append(artifacts.size() - limit).append(" more artifacts.\n");
                }
            }
        }

        memoryBuilder.append("</thread_research_memory>\n");

        return next.next(context).map(prompt -> {
            String baseSystem = prompt.systemPrompt();
            String updatedSystem = (baseSystem == null || baseSystem.isBlank())
                    ? memoryBuilder.toString().trim()
                    : baseSystem + "\n" + memoryBuilder.toString().trim();
            return new ModelPrompt(updatedSystem, prompt.userPrompt(), prompt.modelName());
        });
    }
}
