package org.wrj.haifa.ai.deerflow.completion;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public record CompletionRequirement(
        String requirementId,
        CompletionRequirementType type,
        String subject,
        Freshness freshness,
        Map<String, Object> attributes) {

    public CompletionRequirement {
        requirementId = safe(requirementId);
        type = type == null ? CompletionRequirementType.COMMAND_EXECUTION : type;
        subject = safe(subject);
        freshness = freshness == null ? Freshness.CURRENT_RUN : freshness;
        attributes = immutable(attributes);
    }

    public Map<String, Object> toMap() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("requirementId", requirementId);
        value.put("type", type.name());
        value.put("subject", subject);
        value.put("freshness", freshness.name());
        value.put("attributes", attributes);
        return Map.copyOf(value);
    }

    public static Optional<CompletionRequirement> fromMap(Map<String, Object> value) {
        if (value == null || value.isEmpty()) {
            return Optional.empty();
        }
        try {
            CompletionRequirementType type = CompletionRequirementType.valueOf(
                    safe(value.get("type")).trim().toUpperCase(java.util.Locale.ROOT));
            Freshness freshness = parseFreshness(value.get("freshness"));
            Map<String, Object> attributes = mapValue(value.get("attributes"));
            return Optional.of(new CompletionRequirement(
                    safe(value.get("requirementId")), type, safe(value.get("subject")), freshness, attributes));
        } catch (RuntimeException ex) {
            return Optional.empty();
        }
    }

    private static Freshness parseFreshness(Object value) {
        String raw = safe(value).trim();
        return raw.isEmpty() ? Freshness.CURRENT_RUN
                : Freshness.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
    }

    private static Map<String, Object> mapValue(Object value) {
        if (!(value instanceof Map<?, ?> source)) {
            return Map.of();
        }
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static Map<String, Object> immutable(Map<String, Object> value) {
        return value == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(value));
    }

    private static String safe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
