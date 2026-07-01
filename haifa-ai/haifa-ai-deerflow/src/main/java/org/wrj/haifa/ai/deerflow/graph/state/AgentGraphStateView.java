package org.wrj.haifa.ai.deerflow.graph.state;

import com.alibaba.cloud.ai.graph.OverAllState;
import org.wrj.haifa.ai.deerflow.agent.RunMode;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class AgentGraphStateView {

    private final Map<String, Object> state;

    private AgentGraphStateView(Map<String, Object> state) {
        this.state = state == null ? Map.of() : Map.copyOf(state);
    }

    public static AgentGraphStateView of(Map<String, Object> state) {
        return new AgentGraphStateView(state);
    }

    public static AgentGraphStateView of(OverAllState state) {
        return new AgentGraphStateView(state == null ? Map.of() : state.data());
    }

    public Map<String, Object> data() {
        return state;
    }

    public String runId() {
        return string(AgentGraphStateKeys.RUN_ID).orElse("");
    }

    public String threadId() {
        return string(AgentGraphStateKeys.THREAD_ID).orElse("");
    }

    public RunMode mode() {
        return string(AgentGraphStateKeys.MODE)
                .map(value -> {
                    try {
                        return RunMode.valueOf(value);
                    }
                    catch (IllegalArgumentException ignored) {
                        return RunMode.CHAT;
                    }
                })
                .orElse(RunMode.CHAT);
    }

    public String userMessage() {
        return string(AgentGraphStateKeys.USER_MESSAGE).orElse("");
    }

    public String modelName() {
        return string(AgentGraphStateKeys.MODEL_NAME).orElse("");
    }

    public List<Map<String, Object>> messageWindow() {
        return listOfMaps(AgentGraphStateKeys.MESSAGE_WINDOW);
    }

    public List<Map<String, Object>> modelSteps() {
        return listOfMaps(AgentGraphStateKeys.MODEL_STEPS);
    }

    public List<Map<String, Object>> toolCalls() {
        return listOfMaps(AgentGraphStateKeys.TOOL_CALLS);
    }

    public List<Map<String, Object>> toolResults() {
        return listOfMaps(AgentGraphStateKeys.TOOL_RESULTS);
    }

    public List<Map<String, Object>> artifacts() {
        return listOfMaps(AgentGraphStateKeys.ARTIFACTS);
    }

    public String finalAnswer() {
        return string(AgentGraphStateKeys.FINAL_ANSWER).orElse("");
    }

    public Optional<String> string(String key) {
        Object value = state.get(key);
        return value instanceof String text ? Optional.of(text) : Optional.empty();
    }

    public Map<String, Object> map(String key) {
        Object value = state.get(key);
        if (value instanceof Map<?, ?> raw) {
            return copyStringObjectMap(raw);
        }
        return Map.of();
    }

    public List<Object> list(String key) {
        Object value = state.get(key);
        if (value instanceof List<?> raw) {
            return List.copyOf(raw);
        }
        return List.of();
    }

    public List<Map<String, Object>> listOfMaps(String key) {
        return list(key).stream()
                .filter(Map.class::isInstance)
                .map(Map.class::cast)
                .map(AgentGraphStateView::copyStringObjectMap)
                .toList();
    }

    private static Map<String, Object> copyStringObjectMap(Map<?, ?> raw) {
        return raw.entrySet().stream()
                .filter(entry -> entry.getKey() instanceof String)
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        entry -> (String) entry.getKey(),
                        Map.Entry::getValue,
                        (left, right) -> right
                ));
    }
}
