package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.completion.CompletionRequirement;
import org.wrj.haifa.ai.deerflow.completion.CompletionRequirementType;
import org.wrj.haifa.ai.deerflow.completion.EvidenceRecord;
import org.wrj.haifa.ai.deerflow.completion.EvidenceType;
import org.wrj.haifa.ai.deerflow.completion.Freshness;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;

import static org.assertj.core.api.Assertions.assertThat;

class ChatFinalAnswerGateNodeTest {

    @Test
    void acceptsOrdinaryAnswerWhenRunHasNoCompletionRequirements() {
        Map<String, Object> update = node().apply(state(
                "图片已成功生成，当前电量 91%。",
                List.of(),
                List.of())).join();

        assertThat(update).containsEntry("final_answer_gate_status", "ACCEPTED");
    }

    @Test
    void rejectsMissingArtifactEvidenceWithoutInspectingAnswerText() {
        Map<String, Object> update = node().apply(state(
                "done",
                List.of(requirement("artifact", CompletionRequirementType.ARTIFACT_DELIVERY)),
                List.of())).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
        assertThat((List<?>) update.get(AgentGraphStateKeys.MESSAGE_WINDOW)).isNotEmpty();
    }

    @Test
    void artifactEvidenceCannotSatisfyLocalObservationRequirement() {
        Map<String, Object> update = node().apply(state(
                "当前数据如下。",
                List.of(requirement("observation", CompletionRequirementType.LOCAL_OBSERVATION)),
                List.of(evidence("artifact-1", EvidenceType.ARTIFACT, "run-1")))).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
    }

    @Test
    void acceptsRequirementWithMatchingCurrentRunEvidence() {
        Map<String, Object> update = node().apply(state(
                "当前电量为 91%。",
                List.of(requirement("observation", CompletionRequirementType.LOCAL_OBSERVATION)),
                List.of(evidence("measurement-1", EvidenceType.MEASUREMENT, "run-1")))).join();

        assertThat(update).containsEntry("final_answer_gate_status", "ACCEPTED");
    }

    @Test
    void evidenceFromAnotherRunDoesNotSatisfyCurrentRunRequirement() {
        Map<String, Object> update = node().apply(state(
                "当前电量为 91%。",
                List.of(requirement("observation", CompletionRequirementType.LOCAL_OBSERVATION)),
                List.of(evidence("measurement-old", EvidenceType.MEASUREMENT, "run-old")))).join();

        assertThat(update).containsEntry("final_answer_gate_status", "CONTINUE");
    }

    private static ChatFinalAnswerGateNode node() {
        return new ChatFinalAnswerGateNode();
    }

    private static OverAllState state(String answer, List<Map<String, Object>> requirements,
            List<Map<String, Object>> evidence) {
        return new OverAllState(Map.of(
                AgentGraphStateKeys.RUN_ID, "run-1",
                AgentGraphStateKeys.THREAD_ID, "thread-1",
                "last_assistant_content", answer,
                AgentGraphStateKeys.COMPLETION_REQUIREMENTS, requirements,
                AgentGraphStateKeys.EVIDENCE_LEDGER, evidence));
    }

    private static Map<String, Object> requirement(String id, CompletionRequirementType type) {
        return new CompletionRequirement(id, type, "test", Freshness.CURRENT_RUN, Map.of()).toMap();
    }

    private static Map<String, Object> evidence(String id, EvidenceType type, String runId) {
        return new EvidenceRecord(id, type, runId, "call-1", "tool-result:call-1", "a".repeat(64),
                Instant.parse("2026-07-16T00:00:00Z"), List.of(), Map.of()).toMap();
    }
}
