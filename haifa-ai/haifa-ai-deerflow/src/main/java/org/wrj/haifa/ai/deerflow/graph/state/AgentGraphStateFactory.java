package org.wrj.haifa.ai.deerflow.graph.state;

import org.wrj.haifa.ai.deerflow.agent.AgentRequest;
import org.wrj.haifa.ai.deerflow.agent.AgentRunConfig;
import org.wrj.haifa.ai.deerflow.model.ModelPrompt;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class AgentGraphStateFactory {

    public static final int DEFAULT_MESSAGE_WINDOW_LIMIT = 20;
    public static final int DEFAULT_MAX_TEXT_CHARS = 8_000;

    private final int messageWindowLimit;
    private final int maxTextChars;

    public AgentGraphStateFactory() {
        this(DEFAULT_MESSAGE_WINDOW_LIMIT, DEFAULT_MAX_TEXT_CHARS);
    }

    public AgentGraphStateFactory(int messageWindowLimit, int maxTextChars) {
        this.messageWindowLimit = Math.max(1, messageWindowLimit);
        this.maxTextChars = Math.max(1, maxTextChars);
    }

    public Map<String, Object> create(AgentRunConfig config, AgentRequest request, List<MessageRecord> threadHistory) {
        return create(config, request, threadHistory, null);
    }

    public Map<String, Object> create(AgentRunConfig config, AgentRequest request, List<MessageRecord> threadHistory,
            ModelPrompt prompt) {
        Objects.requireNonNull(config, "config must not be null");
        Objects.requireNonNull(request, "request must not be null");

        Map<String, Object> state = new LinkedHashMap<>();
        state.put(AgentGraphStateKeys.RUN_ID, safe(config.runId()));
        state.put(AgentGraphStateKeys.THREAD_ID, safe(config.threadId()));
        state.put(AgentGraphStateKeys.MODE, config.mode().name());
        state.put(AgentGraphStateKeys.USER_ID, safe(request.userId()));
        state.put(AgentGraphStateKeys.USER_MESSAGE, truncate(request.message()));
        state.put(AgentGraphStateKeys.MODEL_NAME, safe(config.modelName()));
        state.put(AgentGraphStateKeys.UPLOADED_FILE_IDS, List.copyOf(request.uploadedFileIds()));
        state.put(AgentGraphStateKeys.REQUEST_METADATA, Map.copyOf(request.metadata()));
        state.put(AgentGraphStateKeys.MESSAGE_WINDOW, messageWindow(threadHistory));
        state.put(AgentGraphStateKeys.MODEL_PROMPT, modelPrompt(prompt));
        state.put(AgentGraphStateKeys.MODEL_STEPS, List.of());
        state.put(AgentGraphStateKeys.TOOL_CALLS, List.of());
        state.put(AgentGraphStateKeys.TOOL_RESULTS, List.of());
        state.put(AgentGraphStateKeys.PENDING_TOOL_CALLS, List.of());
        state.put(AgentGraphStateKeys.TODOS, Map.of());
        state.put(AgentGraphStateKeys.RESEARCH_PLAN_REF, Map.of());
        state.put("researchOptions", config.researchOptions());
        state.put(AgentGraphStateKeys.RESEARCH_PHASE, "");
        state.put(AgentGraphStateKeys.SUBAGENTS, Map.of());
        state.put(AgentGraphStateKeys.CLARIFICATION, Map.of());
        state.put(AgentGraphStateKeys.SANDBOX, Map.of());
        state.put(AgentGraphStateKeys.ARTIFACTS, List.of());
        state.put(AgentGraphStateKeys.ERRORS, List.of());
        state.put(AgentGraphStateKeys.FINAL_ANSWER, "");
        state.put(AgentGraphStateKeys.USAGE, Map.of());
        return Map.copyOf(state);
    }

    private List<Map<String, Object>> messageWindow(List<MessageRecord> threadHistory) {
        List<MessageRecord> messages = threadHistory == null ? List.of() : threadHistory;
        int from = Math.max(0, messages.size() - messageWindowLimit);
        return messages.subList(from, messages.size()).stream()
                .map(this::messageRef)
                .toList();
    }

    private Map<String, Object> messageRef(MessageRecord message) {
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("messageId", safe(message.messageId()));
        ref.put("threadId", safe(message.threadId()));
        ref.put("runId", safe(message.runId()));
        ref.put("role", message.role() == null ? "" : message.role().name());
        ref.put("content", truncate(message.content()));
        ref.put("metadata", message.metadata() == null ? Map.of() : Map.copyOf(message.metadata()));
        ref.put("createdAt", instant(message.createdAt()));
        return Map.copyOf(ref);
    }

    private Map<String, Object> modelPrompt(ModelPrompt prompt) {
        if (prompt == null) {
            return Map.of();
        }
        Map<String, Object> ref = new LinkedHashMap<>();
        ref.put("systemPrompt", truncate(prompt.systemPrompt()));
        ref.put("userPrompt", truncate(prompt.userPrompt()));
        ref.put("modelName", safe(prompt.modelName()));
        return Map.copyOf(ref);
    }

    private String truncate(String value) {
        String safeValue = safe(value);
        if (safeValue.length() <= maxTextChars) {
            return safeValue;
        }
        return safeValue.substring(0, maxTextChars);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String instant(Instant value) {
        return value == null ? "" : value.toString();
    }
}
