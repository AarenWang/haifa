package org.wrj.haifa.ai.deerflow.graph.state;

import java.util.LinkedHashMap;
import java.util.Map;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;

/** Maps the derived model prompt to and from checkpoint-safe graph state. */
public final class GraphPromptStateMapper {

    private GraphPromptStateMapper() {
    }

    public static Map<String, Object> toState(ModelPrompt prompt) {
        if (prompt == null) {
            return Map.of();
        }
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("systemPrompt", prompt.systemPrompt());
        state.put("userPrompt", prompt.userPrompt());
        state.put("modelName", prompt.modelName() == null ? "" : prompt.modelName());
        return Map.copyOf(state);
    }

    public static ModelPrompt fromState(Map<String, Object> state, String fallbackModelName) {
        Map<String, Object> safe = state == null ? Map.of() : state;
        return new ModelPrompt(
                stringValue(safe.get("systemPrompt")),
                stringValue(safe.get("userPrompt")),
                firstNonBlank(stringValue(safe.get("modelName")), fallbackModelName));
    }

    private static String stringValue(Object value) {
        return value instanceof String text ? text : "";
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? (second == null ? "" : second) : first;
    }
}
