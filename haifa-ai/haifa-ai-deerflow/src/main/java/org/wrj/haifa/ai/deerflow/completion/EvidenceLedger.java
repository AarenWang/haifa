package org.wrj.haifa.ai.deerflow.completion;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EvidenceLedger {

    private static final Map<CompletionRequirementType, Set<EvidenceType>> SATISFYING_TYPES = satisfyingTypes();
    private final String runId;
    private final List<EvidenceRecord> records;

    public EvidenceLedger(String runId, List<EvidenceRecord> records) {
        this.runId = runId == null ? "" : runId;
        this.records = records == null ? List.of() : List.copyOf(records);
    }

    public boolean satisfies(CompletionRequirement requirement) {
        if (requirement == null) {
            return true;
        }
        Set<EvidenceType> accepted = SATISFYING_TYPES.getOrDefault(requirement.type(), Set.of());
        return records.stream()
                .filter(this::isTrustedRuntimeEvidence)
                .filter(record -> accepted.contains(record.type()))
                .filter(record -> requirement.freshness() != Freshness.CURRENT_RUN || runId.equals(record.runId()))
                .anyMatch(record -> lineageIsValid(record, accepted));
    }

    private boolean isTrustedRuntimeEvidence(EvidenceRecord record) {
        return record != null
                && !record.evidenceId().isBlank()
                && !record.sourceToolCallId().isBlank()
                && record.payloadHash().matches("(?i)[0-9a-f]{64}");
    }

    private boolean lineageIsValid(EvidenceRecord record, Set<EvidenceType> accepted) {
        if (record.type() != EvidenceType.DERIVED_DATASET && record.type() != EvidenceType.ARTIFACT) {
            return true;
        }
        if (record.parentEvidenceIds().isEmpty()) {
            return record.type() == EvidenceType.ARTIFACT && accepted.equals(Set.of(EvidenceType.ARTIFACT));
        }
        Set<String> ids = records.stream().map(EvidenceRecord::evidenceId).collect(java.util.stream.Collectors.toSet());
        return ids.containsAll(record.parentEvidenceIds());
    }

    private static Map<CompletionRequirementType, Set<EvidenceType>> satisfyingTypes() {
        Map<CompletionRequirementType, Set<EvidenceType>> result = new EnumMap<>(CompletionRequirementType.class);
        result.put(CompletionRequirementType.LOCAL_OBSERVATION, Set.of(EvidenceType.MEASUREMENT));
        result.put(CompletionRequirementType.DERIVED_DATA, Set.of(EvidenceType.DERIVED_DATASET));
        result.put(CompletionRequirementType.ARTIFACT_DELIVERY, Set.of(EvidenceType.ARTIFACT));
        result.put(CompletionRequirementType.WEB_CITATION, Set.of(EvidenceType.WEB_SOURCE));
        result.put(CompletionRequirementType.FILE_MUTATION, Set.of(EvidenceType.FILE_CHANGE));
        result.put(CompletionRequirementType.COMMAND_EXECUTION, Set.of(EvidenceType.COMMAND_RESULT));
        result.put(CompletionRequirementType.USER_PROVIDED_DATA, Set.of(EvidenceType.USER_DATA));
        return Map.copyOf(result);
    }
}
