package org.wrj.haifa.ai.deerflow.agent.loop;

import org.wrj.haifa.ai.deerflow.model.ModelMessage;
import org.wrj.haifa.ai.deerflow.model.ModelToolCall;
import java.util.*;

/**
 * Validates and repairs the dialogue history messages before they are sent to the model.
 * Ensures strict tool call and tool result pairing.
 */
public class ConversationReplaySanitizer {

    public List<ModelMessage> sanitize(List<ModelMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<ModelMessage> parsedMessages = new ArrayList<>();
        for (ModelMessage message : messages) {
            if (message != null) {
                parsedMessages.add(message);
            }
        }
        // Strict pairing logic
        List<ModelMessage> sanitized = new ArrayList<>();
        Set<ModelMessage> consumedToolMessages = new HashSet<>();

        for (int i = 0; i < parsedMessages.size(); i++) {
            ModelMessage msg = parsedMessages.get(i);

            if (msg.role() == ModelMessage.Role.ASSISTANT) {
                if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
                    // assistant with tool calls
                    sanitized.add(msg);

                    for (ModelToolCall tc : msg.toolCalls()) {
                        ModelMessage matchedToolMsg = null;

                        // 1. Try to match by toolCallId
                        for (int j = i + 1; j < parsedMessages.size(); j++) {
                            ModelMessage forwardMsg = parsedMessages.get(j);
                            if (forwardMsg.role() == ModelMessage.Role.TOOL && !consumedToolMessages.contains(forwardMsg)) {
                                if (tc.id() != null && tc.id().equals(forwardMsg.toolCallId())) {
                                    matchedToolMsg = forwardMsg;
                                    break;
                                }
                            }
                        }

                        // 2. Fall back to matching by name
                        if (matchedToolMsg == null) {
                            for (int j = i + 1; j < parsedMessages.size(); j++) {
                                ModelMessage forwardMsg = parsedMessages.get(j);
                                if (forwardMsg.role() == ModelMessage.Role.TOOL && !consumedToolMessages.contains(forwardMsg)) {
                                    if (tc.name() != null && tc.name().equalsIgnoreCase(forwardMsg.name())) {
                                        matchedToolMsg = forwardMsg;
                                        break;
                                    }
                                }
                            }
                        }

                        if (matchedToolMsg != null) {
                            consumedToolMessages.add(matchedToolMsg);
                            // Standardize pairing
                            ModelMessage pairedTool = new ModelMessage(
                                matchedToolMsg.role(),
                                matchedToolMsg.content(),
                                matchedToolMsg.toolCalls(),
                                tc.id(),
                                tc.name(),
                                matchedToolMsg.metadata()
                            );
                            sanitized.add(pairedTool);
                        } else {
                            // Missing result -> insert synthetic "aborted" result
                            Map<String, Object> meta = new HashMap<>(msg.metadata());
                            meta.put("status", "FAILED");
                            meta.put("toolCallId", tc.id());
                            meta.put("tool", tc.name());
                            meta.put("synthetic", true);

                            ModelMessage syntheticTool = new ModelMessage(
                                ModelMessage.Role.TOOL,
                                "aborted",
                                List.of(),
                                tc.id(),
                                tc.name(),
                                meta
                            );
                            sanitized.add(syntheticTool);
                        }
                    }
                } else {
                    // Regular assistant message, check if it's empty noise
                    if (msg.content() != null && !msg.content().isBlank()) {
                        sanitized.add(msg);
                    }
                }
            } else if (msg.role() == ModelMessage.Role.TOOL) {
                // Discard stray tool results that were not matched & consumed by an assistant message
                if (consumedToolMessages.contains(msg)) {
                    // already added
                }
            } else {
                // SYSTEM or USER
                sanitized.add(msg);
            }
        }

        // Assistant-First repair
        if (!sanitized.isEmpty() && sanitized.get(0).role() == ModelMessage.Role.ASSISTANT) {
            sanitized.add(0, new ModelMessage(ModelMessage.Role.USER, "Initialize context."));
        }

        return sanitized;
    }
}

