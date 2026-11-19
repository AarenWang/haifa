package org.wrj.haifa.ai.deerflow.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.agent.AgentEvent;
import org.wrj.haifa.ai.deerflow.agent.AgentEventType;
import org.wrj.haifa.ai.deerflow.graph.checkpoint.AgentGraphCheckpointRecord;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentEventStore;
import org.wrj.haifa.ai.deerflow.persistence.store.AgentGraphCheckpointStore;
import org.wrj.haifa.ai.deerflow.research.EvidenceItem;
import org.wrj.haifa.ai.deerflow.research.ResearchRuntimeSupport;
import org.wrj.haifa.ai.deerflow.research.ResearchSource;
import org.wrj.haifa.ai.deerflow.research.ResearchSourceType;
import org.wrj.haifa.ai.deerflow.run.RunManager;
import org.wrj.haifa.ai.deerflow.run.RunRecord;
import org.wrj.haifa.ai.deerflow.run.RunStatus;

class RunObservabilityServiceTest {

    @Test
    void describesRunTimelineCheckpointsAndResearchTraceability() {
        RunManager runManager = mock(RunManager.class);
        AgentEventStore eventStore = mock(AgentEventStore.class);
        AgentGraphCheckpointStore checkpointStore = mock(AgentGraphCheckpointStore.class);
        ResearchRuntimeSupport researchRuntimeSupport = mock(ResearchRuntimeSupport.class);

        String runId = "run-1";
        String threadId = "thread-1";
        Instant now = Instant.now();
        when(runManager.find(runId)).thenReturn(Optional.of(new RunRecord(
                runId, threadId, "model", RunStatus.COMPLETED, null,
                Map.of("mode", "research"), now, now)));
        when(eventStore.findByRunId(runId)).thenReturn(List.of(
                AgentEvent.of("evt-1", runId, threadId, AgentEventType.RUN_STARTED,
                        "started", Map.of("nodeId", "start")),
                AgentEvent.of("evt-2", runId, threadId, AgentEventType.REPORT_COMPLETED,
                        "done", Map.of("nodeId", "write_report", "checkpointId", "cp-2"))
        ));
        when(checkpointStore.findByRunId(runId)).thenReturn(List.of(
                checkpoint("cp-1", runId, threadId, "create_or_load_plan", "search_sources"),
                checkpoint("cp-2", runId, threadId, "write_report", "")
        ));

        ResearchSource source = new ResearchSource("src-1", threadId, runId, "Source",
                "https://example.com", "https://example.com", "example.com", null, now,
                ResearchSourceType.WEB_PAGE, 0.9, "snippet", "hash");
        ResearchSource uncited = new ResearchSource("src-2", threadId, runId, "Uncited",
                "https://example.org", "https://example.org", "example.org", null, null,
                ResearchSourceType.SEARCH_RESULT, 0.5, "snippet", "");
        when(researchRuntimeSupport.listSourcesByRun(runId)).thenReturn(List.of(source, uncited));
        when(researchRuntimeSupport.listEvidenceByRun(runId)).thenReturn(List.of(
                new EvidenceItem("ev-1", threadId, runId, "src-1", "quote", "claim", "facts", 0.8, now),
                new EvidenceItem("ev-orphan", threadId, runId, "missing-src", "quote", "claim", "facts", 0.7, now)
        ));
        when(researchRuntimeSupport.citationCount(runId, "src-1")).thenReturn(1);
        when(researchRuntimeSupport.citationCount(runId, "src-2")).thenReturn(0);

        RunObservabilityService service = new RunObservabilityService(
                runManager, eventStore, checkpointStore, researchRuntimeSupport);

        RunObservabilityService.RunObservabilityResponse response = service.describe(runId).orElseThrow();

        assertThat(response.mode()).isEqualTo("research");
        assertThat(response.graphName()).isEqualTo("haifa-active-research");
        assertThat(response.eventCount()).isEqualTo(2);
        assertThat(response.checkpointCount()).isEqualTo(2);
        assertThat(response.sourceCount()).isEqualTo(2);
        assertThat(response.fetchedSourceCount()).isEqualTo(1);
        assertThat(response.evidenceCount()).isEqualTo(2);
        assertThat(response.orphanEvidenceCount()).isEqualTo(1);
        assertThat(response.citedSourceCount()).isEqualTo(1);
        assertThat(response.citationCoverage()).isEqualTo(0.5);
        assertThat(response.timeline())
                .extracting(RunObservabilityService.TimelineItem::nodeId)
                .containsExactly("start", "write_report");
        assertThat(response.checkpointTimeline())
                .extracting(RunObservabilityService.CheckpointTimelineItem::nodeId)
                .containsExactly("create_or_load_plan", "write_report");
        assertThat(response.researchTraceability().sources().get(0).evidenceIds())
                .containsExactly("ev-1");
    }

    private static AgentGraphCheckpointRecord checkpoint(String checkpointId, String runId,
                                                        String threadId, String nodeId, String nextNodeId) {
        return new AgentGraphCheckpointRecord(
                "record-" + checkpointId,
                checkpointId,
                runId,
                threadId,
                "haifa-active-research",
                nodeId,
                nextNodeId,
                Map.of("nodeId", nodeId, "nextNodeId", nextNodeId),
                Map.of("omitted", true),
                Instant.now()
        );
    }
}
