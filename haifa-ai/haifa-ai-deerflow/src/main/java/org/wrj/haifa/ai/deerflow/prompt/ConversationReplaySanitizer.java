package org.wrj.haifa.ai.deerflow.prompt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;

/** Repairs tool-call/result pairing before a prompt is sent to the model. */
public class ConversationReplaySanitizer {
    public List<ModelMessage> sanitize(List<ModelMessage> messages) {
        List<ModelMessage> source = messages == null ? List.of() : messages.stream().filter(java.util.Objects::nonNull).toList();
        List<ModelMessage> sanitized = new ArrayList<>();
        Set<ModelMessage> consumed = new HashSet<>();
        for (int i = 0; i < source.size(); i++) {
            ModelMessage message = source.get(i);
            if (message.role() == ModelMessage.Role.ASSISTANT && message.toolCalls() != null
                    && !message.toolCalls().isEmpty()) {
                sanitized.add(message);
                for (ModelToolCall call : message.toolCalls()) {
                    ModelMessage match = findToolResult(source, consumed, i + 1, call, true);
                    if (match == null) match = findToolResult(source, consumed, i + 1, call, false);
                    if (match != null) {
                        consumed.add(match);
                        sanitized.add(new ModelMessage(ModelMessage.Role.TOOL, match.content(), match.toolCalls(),
                                call.id(), call.name(), match.metadata()));
                    } else {
                        Map<String, Object> metadata = new HashMap<>(message.metadata());
                        metadata.put("status", "FAILED");
                        metadata.put("synthetic", true);
                        sanitized.add(new ModelMessage(ModelMessage.Role.TOOL, "aborted", List.of(),
                                call.id(), call.name(), metadata));
                    }
                }
            } else if (message.role() != ModelMessage.Role.TOOL
                    && (message.role() != ModelMessage.Role.ASSISTANT || !message.content().isBlank())) {
                sanitized.add(message);
            }
        }
        if (!sanitized.isEmpty() && sanitized.get(0).role() == ModelMessage.Role.ASSISTANT) {
            sanitized.add(0, new ModelMessage(ModelMessage.Role.USER, "Initialize context."));
        }
        return sanitized;
    }

    private static ModelMessage findToolResult(List<ModelMessage> source, Set<ModelMessage> consumed,
            int start, ModelToolCall call, boolean byId) {
        for (int i = start; i < source.size(); i++) {
            ModelMessage candidate = source.get(i);
            if (candidate.role() != ModelMessage.Role.TOOL || consumed.contains(candidate)) continue;
            if (byId ? java.util.Objects.equals(call.id(), candidate.toolCallId())
                    : call.name() != null && call.name().equalsIgnoreCase(candidate.name())) return candidate;
        }
        return null;
    }
}
