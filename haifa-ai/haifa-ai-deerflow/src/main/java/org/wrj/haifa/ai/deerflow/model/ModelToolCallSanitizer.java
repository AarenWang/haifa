package org.wrj.haifa.ai.deerflow.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Keeps model-generated tool arguments valid at execution and provider boundaries. */
public final class ModelToolCallSanitizer {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private ModelToolCallSanitizer() {
    }

    public static String sanitizeArguments(String arguments) {
        if (arguments == null || arguments.isBlank()) {
            return "{}";
        }
        try {
            JsonNode node = OBJECT_MAPPER.readTree(arguments);
            return node != null && node.isObject() ? arguments : "{}";
        } catch (Exception ignored) {
            return "{}";
        }
    }
}
