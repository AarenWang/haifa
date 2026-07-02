package org.wrj.haifa.ai.deerflow.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.AsyncNodeAction;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.graph.state.AgentGraphStateKeys;
import org.wrj.haifa.ai.deerflow.thread.MessageRecord;
import org.wrj.haifa.ai.deerflow.thread.MessageStore;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Component
public class ChatLoadContextNode implements AsyncNodeAction {

    private static final int MESSAGE_WINDOW_LIMIT = 20;
    private static final int MAX_TEXT_CHARS = 8_000;

    private final MessageStore messageStore;

    public ChatLoadContextNode(MessageStore messageStore) {
        this.messageStore = messageStore;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(OverAllState state) {
        return CompletableFuture.supplyAsync(() -> {
            String threadId = state.<String>value(AgentGraphStateKeys.THREAD_ID).orElse("");
            List<MessageRecord> messages = messageStore.listByThread(threadId);
            List<String> existingMessageIds = state.<List<Map<String, Object>>>value(AgentGraphStateKeys.MESSAGE_WINDOW)
                    .orElse(List.of())
                    .stream()
                    .map(message -> String.valueOf(message.getOrDefault("messageId", "")))
                    .toList();

            int from = Math.max(0, messages.size() - MESSAGE_WINDOW_LIMIT);
            List<Map<String, Object>> messageRefs = messages.subList(from, messages.size()).stream()
                    .map(this::messageRef)
                    .filter(message -> !existingMessageIds.contains(String.valueOf(message.getOrDefault("messageId", ""))))
                    .toList();

            Map<String, Object> update = new HashMap<>();
            update.put(AgentGraphStateKeys.MESSAGE_WINDOW, messageRefs);
            update.put(AgentGraphStateKeys.MODEL_STEPS, List.of(Map.of("node", "load_context", "status", "completed")));
            return update;
        });
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

    private String truncate(String value) {
        String safeValue = safe(value);
        if (safeValue.length() <= MAX_TEXT_CHARS) {
            return safeValue;
        }
        return safeValue.substring(0, MAX_TEXT_CHARS);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String instant(Instant value) {
        return value == null ? "" : value.toString();
    }
}
