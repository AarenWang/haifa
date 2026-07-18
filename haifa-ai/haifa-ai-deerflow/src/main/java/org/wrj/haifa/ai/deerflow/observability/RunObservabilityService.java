package org.wrj.haifa.ai.deerflow.observability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.AgentGraphCheckpointRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunStatus;

@Component
public class RunObservabilityService {

    private final RunManager runManager;
    private final AgentEventStore eventStore;
    private final AgentGraphCheckpointStore checkpointStore;
    private final ResearchRuntimeSupport researchRuntimeSupport;

    public RunObservabilityService(RunManager runManager,
                                   AgentEventStore eventStore,
                                   AgentGraphCheckpointStore checkpointStore,
                                   ResearchRuntimeSupport researchRuntimeSupport) {
        this.runManager = runManager;
        this.eventStore = eventStore;
        this.checkpointStore = checkpointStore;
        this.researchRuntimeSupport = researchRuntimeSupport;
    }

    public Optional<RunObservabilityResponse> describe(String runId) {
        return runManager.find(runId).map(run -> {
            List<AgentEvent> events = eventStore.findByRunId(runId);
            List<AgentGraphCheckpointRecord> checkpoints = checkpointStore.findByRunId(runId);
            List<ResearchSource> sources = researchRuntimeSupport.listSourcesByRun(runId);
            List<EvidenceItem> evidence = researchRuntimeSupport.listEvidenceByRun(runId);

            Set<String> sourceIds = sources.stream()
                    .map(ResearchSource::sourceId)
                    .collect(Collectors.toSet());
            int orphanEvidenceCount = (int) evidence.stream()
                    .filter(item -> !sourceIds.contains(item.sourceId()))
                    .count();

            List<SourceTrace> sourceTraces = sources.stream()
                    .map(source -> sourceTrace(runId, source, evidence))
                    .toList();
            int citedSourceCount = (int) sourceTraces.stream()
                    .filter(source -> source.citationCount() > 0 || !source.evidenceIds().isEmpty())
                    .count();
            double citationCoverage = sources.isEmpty() ? 1.0 : (double) citedSourceCount / sources.size();

            String graphName = checkpoints.stream()
                    .map(AgentGraphCheckpointRecord::graphName)
                    .filter(name -> name != null && !name.isBlank())
                    .reduce((first, second) -> second)
                    .orElse("");

            ModelUsageObservability modelUsage = buildModelUsageObservability(runId);

            return new RunObservabilityResponse(
                    run.runId(),
                    run.threadId(),
                    run.mode(),
                    run.status(),
                    graphName,
                    events.size(),
                    checkpoints.size(),
                    sources.size(),
                    (int) sources.stream().filter(ResearchSource::fetched).count(),
                    evidence.size(),
                    orphanEvidenceCount,
                    citedSourceCount,
                    citationCoverage,
                    timeline(events),
                    checkpointTimeline(checkpoints),
                    new ResearchTraceability(sourceTraces, orphanEvidenceCount),
                    modelUsage
            );
        });
    }

    private ModelUsageObservability buildModelUsageObservability(String runId) {
        if (modelStepStore == null) {
            return new ModelUsageObservability(new ModelUsageTotals(0, 0, null, null, null, null, null, null, null), List.of());
        }
        List<org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity> entities = modelStepStore.findByRunId(runId);
        if (entities.isEmpty()) {
            return new ModelUsageObservability(new ModelUsageTotals(0, 0, null, null, null, null, null, null, null), List.of());
        }

        int providerReported = 0;
        int unavailable = 0;
        long totalInput = 0;
        long totalUncachedInput = 0;
        long totalOutput = 0;
        long totalTokensCombined = 0;
        long totalCacheRead = 0;
        long totalCacheWrite = 0;
        boolean hasReportedUsage = false;
        boolean hasInput = false;
        boolean hasUncachedInput = false;
        boolean hasOutput = false;
        boolean hasTotal = false;
        boolean hasCacheRead = false;
        boolean hasCacheWrite = false;

        List<ModelUsageStepDetail> stepDetails = new ArrayList<>();
        org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint prevFingerprint = null;
        String prevModel = null;

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();

        for (org.wrj.haifa.ai.deerflow.persistence.entity.ModelStepEntity entity : entities) {
            org.wrj.haifa.ai.deerflow.model.cache.ModelUsage usage = org.wrj.haifa.ai.deerflow.model.cache.ModelUsage.empty();
            if (entity.getTokenUsageJson() != null && !entity.getTokenUsageJson().isBlank()) {
                try {
                    usage = mapper.readValue(entity.getTokenUsageJson(), org.wrj.haifa.ai.deerflow.model.cache.ModelUsage.class);
                } catch (Exception ignored) {
                }
            }

            String purpose = "AGENT_STEP";
            String eligibility = "UNKNOWN";
            org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint fingerprint = org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint.empty();

            if (entity.getMetadataJson() != null && !entity.getMetadataJson().isBlank()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode metaNode = mapper.readTree(entity.getMetadataJson());
                    if (metaNode.has("purpose")) purpose = metaNode.get("purpose").asText();
                    if (metaNode.has("eligibility")) eligibility = metaNode.get("eligibility").asText();
                    if (metaNode.has("promptFingerprint")) {
                        fingerprint = mapper.treeToValue(metaNode.get("promptFingerprint"), org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint.class);
                    }
                } catch (Exception ignored) {
                }
            }

            boolean isReported = usage.availability() == org.wrj.haifa.ai.deerflow.model.cache.UsageAvailability.PROVIDER_REPORTED;
            if (isReported) {
                providerReported++;
                hasReportedUsage = true;
                if (usage.inputTokens() != null) {
                    totalInput += usage.inputTokens();
                    hasInput = true;
                }
                if (usage.uncachedInputTokens() != null) {
                    totalUncachedInput += usage.uncachedInputTokens();
                    hasUncachedInput = true;
                }
                if (usage.outputTokens() != null) {
                    totalOutput += usage.outputTokens();
                    hasOutput = true;
                }
                if (usage.totalTokens() != null) {
                    totalTokensCombined += usage.totalTokens();
                    hasTotal = true;
                }
                if (usage.cacheReadInputTokens() != null) {
                    totalCacheRead += usage.cacheReadInputTokens();
                    hasCacheRead = true;
                }
                if (usage.cacheWriteInputTokens() != null) {
                    totalCacheWrite += usage.cacheWriteInputTokens();
                    hasCacheWrite = true;
                }
            } else {
                unavailable++;
            }

            String currentModel = usage.model();
            String inferredMissReason = inferMissReason(usage, eligibility, prevFingerprint, fingerprint, prevModel, currentModel);
            prevFingerprint = fingerprint;
            prevModel = currentModel;

            stepDetails.add(new ModelUsageStepDetail(
                    entity.getStepIndex(),
                    purpose,
                    usage.provider(),
                    usage.model(),
                    usage.availability() != null ? usage.availability().name() : "UNAVAILABLE",
                    usage.inputTokens(),
                    usage.outputTokens(),
                    usage.cacheReadInputTokens(),
                    usage.cacheWriteInputTokens(),
                    usage.cacheHitRate(),
                    eligibility,
                    shortHash(fingerprint.cacheablePrefixHash()),
                    shortHash(fingerprint.toolDefinitionsHash()),
                    shortHash(fingerprint.skillCatalogHash()),
                    shortHash(fingerprint.dynamicTailHash()),
                    inferredMissReason
            ));
        }

        Double weightedCacheHitRate = (hasReportedUsage && totalInput > 0 && hasCacheRead)
                ? Math.min(1.0d, (double) totalCacheRead / totalInput)
                : null;

        ModelUsageTotals totals = new ModelUsageTotals(
                providerReported,
                unavailable,
                hasInput ? totalInput : null,
                hasUncachedInput ? totalUncachedInput : null,
                hasOutput ? totalOutput : null,
                hasTotal ? totalTokensCombined : null,
                hasCacheRead ? totalCacheRead : null,
                hasCacheWrite ? totalCacheWrite : null,
                weightedCacheHitRate
        );

        return new ModelUsageObservability(totals, stepDetails);
    }

    private static String inferMissReason(
            org.wrj.haifa.ai.deerflow.model.cache.ModelUsage usage,
            String eligibility,
            org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint prev,
            org.wrj.haifa.ai.deerflow.model.cache.PromptFingerprint curr,
            String prevModel,
            String currModel) {

        if (usage.cacheReadInputTokens() != null && usage.cacheReadInputTokens() > 0) {
            return "NONE";
        }
        if (usage.availability() == org.wrj.haifa.ai.deerflow.model.cache.UsageAvailability.UNAVAILABLE) {
            return "PROVIDER_USAGE_UNAVAILABLE";
        }
        if ("BELOW_MINIMUM".equalsIgnoreCase(eligibility)) {
            return "BELOW_PROVIDER_MINIMUM";
        }
        if (prev == null) {
            return "NO_PRIOR_COMPARABLE_REQUEST";
        }
        if (prevModel != null && currModel != null && !prevModel.equalsIgnoreCase(currModel)) {
            return "MODEL_CHANGED";
        }
        if (!safeStr(prev.staticSystemHash()).equals(safeStr(curr.staticSystemHash()))) {
            return "STATIC_SYSTEM_CHANGED";
        }
        if (!safeStr(prev.toolDefinitionsHash()).equals(safeStr(curr.toolDefinitionsHash()))) {
            return "TOOLS_CHANGED";
        }
        if (!safeStr(prev.skillCatalogHash()).equals(safeStr(curr.skillCatalogHash()))) {
            return "SKILL_CATALOG_CHANGED";
        }
        if (!safeStr(prev.activeSkillsHash()).equals(safeStr(curr.activeSkillsHash()))) {
            return "ACTIVE_SKILLS_CHANGED";
        }
        if (!safeStr(prev.sessionContextHash()).equals(safeStr(curr.sessionContextHash()))) {
            return "SESSION_CONTEXT_CHANGED";
        }
        if (!safeStr(prev.conversationPrefixHash()).equals(safeStr(curr.conversationPrefixHash()))) {
            return "CONVERSATION_PREFIX_CHANGED";
        }
        return "UNKNOWN";
    }

    private static String safeStr(String str) {
        return str == null ? "" : str;
    }

    private static String shortHash(String hash) {
        if (hash == null || hash.isBlank()) return "";
        return hash.length() > 12 ? hash.substring(0, 12) : hash;
    }

    private SourceTrace sourceTrace(String runId, ResearchSource source, List<EvidenceItem> evidence) {
        List<String> evidenceIds = evidence.stream()
                .filter(item -> source.sourceId().equals(item.sourceId()))
                .map(EvidenceItem::evidenceId)
                .toList();
        return new SourceTrace(
                source.sourceId(),
                source.title(),
                source.url(),
                source.domain(),
                source.fetched(),
                source.credibility(),
                researchRuntimeSupport.citationCount(runId, source.sourceId()),
                evidenceIds
        );
    }

    private static List<TimelineItem> timeline(List<AgentEvent> events) {
        List<TimelineItem> timeline = new ArrayList<>(events.size());
        for (int i = 0; i < events.size(); i++) {
            AgentEvent event = events.get(i);
            Map<String, Object> metadata = event.metadata() == null ? Map.of() : event.metadata();
            timeline.add(new TimelineItem(
                    i + 1,
                    event.type().name(),
                    event.content(),
                    text(metadata.get("nodeId")),
                    text(metadata.get("checkpointId")),
                    metadata,
                    event.createdAt()
            ));
        }
        return timeline;
    }

    private static List<CheckpointTimelineItem> checkpointTimeline(List<AgentGraphCheckpointRecord> checkpoints) {
        return checkpoints.stream()
                .map(checkpoint -> new CheckpointTimelineItem(
                        checkpoint.checkpointId(),
                        checkpoint.graphName(),
                        checkpoint.nodeId(),
                        checkpoint.nextNodeId(),
                        checkpoint.stateSummary() == null ? Map.of() : checkpoint.stateSummary(),
                        checkpoint.createdAt()
                ))
                .toList();
    }

    private static String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private org.wrj.haifa.ai.deerflow.persistence.store.ModelStepStore modelStepStore;

    public record RunObservabilityResponse(
            String runId,
            String threadId,
            String mode,
            RunStatus status,
            String graphName,
            int eventCount,
            int checkpointCount,
            int sourceCount,
            int fetchedSourceCount,
            int evidenceCount,
            int orphanEvidenceCount,
            int citedSourceCount,
            double citationCoverage,
            List<TimelineItem> timeline,
            List<CheckpointTimelineItem> checkpointTimeline,
            ResearchTraceability researchTraceability,
            ModelUsageObservability modelUsage
    ) {
    }

    public record ModelUsageObservability(
            ModelUsageTotals totals,
            List<ModelUsageStepDetail> steps
    ) {
    }

    public record ModelUsageTotals(
            int providerReportedSteps,
            int unavailableSteps,
            Long inputTokens,
            Long uncachedInputTokens,
            Long outputTokens,
            Long totalTokens,
            Long cacheReadInputTokens,
            Long cacheWriteInputTokens,
            Double weightedCacheHitRate
    ) {
    }

    public record ModelUsageStepDetail(
            int stepIndex,
            String purpose,
            String provider,
            String model,
            String availability,
            Long inputTokens,
            Long outputTokens,
            Long cacheReadInputTokens,
            Long cacheWriteInputTokens,
            Double cacheHitRate,
            String eligibility,
            String cacheablePrefixHashShort,
            String toolDefinitionsHashShort,
            String skillCatalogHashShort,
            String dynamicTailHashShort,
            String inferredMissReason
    ) {
    }


    public record TimelineItem(
            int sequence,
            String type,
            String content,
            String nodeId,
            String checkpointId,
            Map<String, Object> metadata,
            Instant createdAt
    ) {
    }

    public record CheckpointTimelineItem(
            String checkpointId,
            String graphName,
            String nodeId,
            String nextNodeId,
            Map<String, Object> stateSummary,
            Instant createdAt
    ) {
    }

    public record ResearchTraceability(
            List<SourceTrace> sources,
            int orphanEvidenceCount
    ) {
    }

    public record SourceTrace(
            String sourceId,
            String title,
            String url,
            String domain,
            boolean fetched,
            double credibility,
            int citationCount,
            List<String> evidenceIds
    ) {
    }
}
