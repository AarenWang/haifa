package org.wrj.haifa.ai.deerflow.completion;

import java.util.Map;
import java.util.Optional;

public record ToolCompletionContract(
        CompletionRequirementType requirementType,
        EvidenceType successEvidenceType,
        String subject) {

    public ToolCompletionContract {
        if (requirementType == null) {
            throw new IllegalArgumentException("requirementType is required");
        }
        if (successEvidenceType == null) {
            throw new IllegalArgumentException("successEvidenceType is required");
        }
        subject = subject == null ? "" : subject;
    }

    public Map<String, Object> toMap() {
        return Map.of(
                "requirementType", requirementType.name(),
                "successEvidenceType", successEvidenceType.name(),
                "subject", subject);
    }

    public static Optional<ToolCompletionContract> fromMap(Map<?, ?> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            return Optional.of(new ToolCompletionContract(
                    CompletionRequirementType.valueOf(string(value.get("requirementType")).toUpperCase(java.util.Locale.ROOT)),
                    EvidenceType.valueOf(string(value.get("successEvidenceType")).toUpperCase(java.util.Locale.ROOT)),
                    string(value.get("subject"))));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }
}
