package org.wrj.haifa.ai.deerflow.completion;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public record EvidenceRecord(
        String evidenceId,
        EvidenceType type,
        String runId,
        String sourceToolCallId,
        String payloadRef,
        String payloadHash,
        Instant observedAt,
        List<String> parentEvidenceIds,
        Map<String, Object> attributes) {

    public EvidenceRecord {
        evidenceId = safe(evidenceId);
        type = type == null ? EvidenceType.COMMAND_RESULT : type;
        runId = safe(runId);
        sourceToolCallId = safe(sourceToolCallId);
        payloadRef = safe(payloadRef);
        payloadHash = safe(payloadHash);
        observedAt = observedAt == null ? Instant.now() : observedAt;
        parentEvidenceIds = parentEvidenceIds == null ? List.of() : List.copyOf(parentEvidenceIds);
        attributes = attributes == null ? Map.of()
                : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }

    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("evidenceId", evidenceId);
        value.put("type", type.name());
        value.put("runId", runId);
        value.put("sourceToolCallId", sourceToolCallId);
        value.put("payloadRef", payloadRef);
        value.put("payloadHash", payloadHash);
        value.put("observedAt", observedAt.toString());
        value.put("parentEvidenceIds", parentEvidenceIds);
        value.put("attributes", attributes);
        return Map.copyOf(value);
    }

    public static Optional<EvidenceRecord> fromMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            EvidenceType type = EvidenceType.valueOf(
                    safe(value.get("type")).trim().toUpperCase(java.util.Locale.ROOT));
            return Optional.of(new EvidenceRecord(
                    safe(value.get("evidenceId")), type, safe(value.get("runId")),
                    safe(value.get("sourceToolCallId")), safe(value.get("payloadRef")),
                    safe(value.get("payloadHash")), instant(value.get("observedAt")),
                    stringList(value.get("parentEvidenceIds")), mapValue(value.get("attributes"))));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static Instant instant(Object value) {
        String raw = safe(value);
        return raw.isBlank() ? Instant.EPOCH : Instant.parse(raw);
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> source)) {
            return List.of();
        }
        return source.stream().map(EvidenceRecord::safe).filter(item -> !item.isBlank()).toList();
    }

    private static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
