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
                    new ResearchTraceability(sourceTraces, orphanEvidenceCount)
            );
        });
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
            ResearchTraceability researchTraceability
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
