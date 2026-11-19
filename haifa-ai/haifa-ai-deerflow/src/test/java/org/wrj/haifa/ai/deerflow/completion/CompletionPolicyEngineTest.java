package org.wrj.haifa.ai.deerflow.completion;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CompletionPolicyEngineTest {

    private final CompletionPolicyEngine engine = new CompletionPolicyEngine();

    @Test
    void evaluatesRequirementsByEvidenceTypeInsteadOfPayloadText() {
        CompletionRequirement requirement = requirement(CompletionRequirementType.LOCAL_OBSERVATION);
        EvidenceRecord command = evidence("command", EvidenceType.COMMAND_RESULT, "run-1", List.of());

        CompletionPolicyEngine.Decision decision = engine.evaluate(
                "run-1", List.of(requirement.toMap()), List.of(command.toMap()));

        assertThat(decision.allowed()).isFalse();
        assertThat(decision.missingTypes()).containsExactly("LOCAL_OBSERVATION");
    }

    @Test
    void derivedEvidenceRequiresExistingParentEvidence() {
        CompletionRequirement requirement = requirement(CompletionRequirementType.DERIVED_DATA);
        EvidenceRecord derived = evidence("derived", EvidenceType.DERIVED_DATASET, "run-1", List.of("measurement"));

        assertThat(engine.evaluate("run-1", List.of(requirement.toMap()), List.of(derived.toMap())).allowed()).isFalse();

        EvidenceRecord measurement = evidence("measurement", EvidenceType.MEASUREMENT, "run-1", List.of());
        assertThat(engine.evaluate("run-1", List.of(requirement.toMap()),
                List.of(measurement.toMap(), derived.toMap())).allowed()).isTrue();
    }

    @Test
    void repeatedDeclarationOfSameRequirementIsIdempotent() {
        CompletionRequirement requirement = requirement(CompletionRequirementType.LOCAL_OBSERVATION);
        EvidenceRecord measurement = evidence("measurement", EvidenceType.MEASUREMENT, "run-1", List.of());

        CompletionPolicyEngine.Decision decision = engine.evaluate("run-1",
                List.of(requirement.toMap(), requirement.toMap()), List.of(measurement.toMap()));

        assertThat(decision.allowed()).isTrue();
    }

    private static CompletionRequirement requirement(CompletionRequirementType type) {
        return new CompletionRequirement("req-" + type.name(), type, "test", Freshness.CURRENT_RUN, Map.of());
    }

    private static EvidenceRecord evidence(String id, EvidenceType type, String runId, List<String> parents) {
        return new EvidenceRecord(id, type, runId, "call-1", "tool-result:call-1", "a".repeat(64), Instant.EPOCH,
                parents, Map.of());
    }
}
