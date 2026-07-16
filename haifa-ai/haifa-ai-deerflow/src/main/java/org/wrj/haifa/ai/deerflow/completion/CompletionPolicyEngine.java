package org.wrj.haifa.ai.deerflow.completion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class CompletionPolicyEngine {

    public Decision evaluate(String runId, List<Map<String, Object>> requirementValues,
            List<Map<String, Object>> evidenceValues) {
        List<Map<String, Object>> rawRequirements = requirementValues == null ? List.of() : requirementValues;
        List<CompletionRequirement> parsedRequirements = rawRequirements.stream()
                .map(CompletionRequirement::fromMap)
                .flatMap(java.util.Optional::stream)
                .toList();
        if (parsedRequirements.size() != rawRequirements.size()) {
            return new Decision(false, List.of(), List.of("INVALID_REQUIREMENT_RECORD"));
        }
        List<CompletionRequirement> requirements = parsedRequirements.stream()
                .collect(java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toMap(
                                this::identity, requirement -> requirement,
                                (first, ignored) -> first, LinkedHashMap::new),
                        values -> List.copyOf(values.values())));
        List<EvidenceRecord> evidence = evidenceValues == null ? List.of()
                : evidenceValues.stream().map(EvidenceRecord::fromMap).flatMap(java.util.Optional::stream).toList();
        EvidenceLedger ledger = new EvidenceLedger(runId, evidence);
        List<CompletionRequirement> missing = requirements.stream().filter(requirement -> !ledger.satisfies(requirement)).toList();
        return new Decision(missing.isEmpty(), missing, List.of());
    }

    private String identity(CompletionRequirement requirement) {
        if (!requirement.requirementId().isBlank()) {
            return requirement.requirementId();
        }
        return requirement.type().name() + ':' + requirement.subject();
    }

    public record Decision(boolean allowed, List<CompletionRequirement> missingRequirements, List<String> violations) {
        public Decision {
            missingRequirements = missingRequirements == null ? List.of() : List.copyOf(missingRequirements);
            violations = violations == null ? List.of() : List.copyOf(violations);
        }

        public List<String> missingTypes() {
            java.util.LinkedHashSet<String> values = missingRequirements.stream()
                    .map(requirement -> requirement.type().name())
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
            values.addAll(violations);
            return List.copyOf(values);
        }
    }
}
