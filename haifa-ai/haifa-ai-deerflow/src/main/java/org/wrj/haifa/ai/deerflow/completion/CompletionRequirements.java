package org.wrj.haifa.ai.deerflow.completion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompletionRequirements {

    public static final String METADATA_KEY = "completionRequirements";

    private CompletionRequirements() {
    }

    public static List<CompletionRequirement> fromRequestMetadata(Map<String, Object> metadata) {
        if (metadata == null || !(metadata.get(METADATA_KEY) instanceof List<?> values)) {
            return List.of();
        }
        List<CompletionRequirement> result = new ArrayList<>();
        for (int index = 0; index < values.size(); index++) {
            CompletionRequirement requirement = parse(values.get(index), index);
            if (requirement != null) {
                result.add(requirement);
            }
        }
        return List.copyOf(result);
    }

    private static CompletionRequirement parse(Object value, int index) {
        if (value instanceof String typeName) {
            CompletionRequirementType type = type(typeName);
            return type == null ? null : new CompletionRequirement(
                    "req:request:" + index + ':' + type.name(), type, "request", Freshness.CURRENT_RUN, Map.of());
        }
        if (!(value instanceof Map<?, ?> raw)) {
            return null;
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, item) -> normalized.put(String.valueOf(key), item));
        CompletionRequirementType type = type(normalized.get("type"));
        if (type == null) {
            return null;
        }
        String requirementId = string(normalized.get("requirementId"));
        if (requirementId.isBlank()) {
            requirementId = "req:request:" + index + ':' + type.name();
        }
        Freshness freshness = freshness(normalized.get("freshness"));
        Map<String, Object> attributes = normalized.get("attributes") instanceof Map<?, ?> map
                ? stringKeyMap(map) : Map.of();
        return new CompletionRequirement(requirementId, type, string(normalized.get("subject")), freshness, attributes);
    }

    private static CompletionRequirementType type(Object value) {
        try {
            return CompletionRequirementType.valueOf(string(value).trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static Freshness freshness(Object value) {
        String raw = string(value).trim();
        if (raw.isEmpty()) {
            return Freshness.CURRENT_RUN;
        }
        try {
            return Freshness.valueOf(raw.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return Freshness.CURRENT_RUN;
        }
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, item) -> result.put(String.valueOf(key), item));
        return result;
    }

    private static String string(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
