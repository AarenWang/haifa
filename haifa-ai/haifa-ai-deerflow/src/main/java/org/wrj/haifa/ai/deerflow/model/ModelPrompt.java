package org.wrj.haifa.ai.deerflow.model;

import java.util.List;

public record ModelPrompt(
    String systemPrompt,
    String userPrompt,
    String modelName,
    List<ModelMessage> messages,
    List<ModelToolDefinition> toolDefinitions
) {
    public ModelPrompt {
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        userPrompt = userPrompt == null ? "" : userPrompt;
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolDefinitions = toolDefinitions == null ? List.of() : List.copyOf(toolDefinitions);
    }

    public ModelPrompt(String systemPrompt, String userPrompt, String modelName) {
        this(systemPrompt, userPrompt, modelName, List.of(), List.of());
    }

    public ModelPrompt(String systemPrompt, String userPrompt, String modelName, List<ModelMessage> messages) {
        this(systemPrompt, userPrompt, modelName, messages, List.of());
    }

    public boolean hasMessages() {
        return !messages.isEmpty();
    }

    public String effectiveUserPrompt() {
        if (!hasMessages()) {
            return userPrompt;
        }
        return renderMessages(messages);
    }

    public ModelPrompt withToolDefinitions(List<ModelToolDefinition> toolDefinitions) {
        return new ModelPrompt(systemPrompt, userPrompt, modelName, messages, toolDefinitions);
    }

    public static String renderMessages(List<ModelMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ModelMessage message : messages) {
            if (message == null) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append("\n\n");
            }
            switch (message.role()) {
                case SYSTEM -> sb.append("System: ").append(message.content());
                case USER -> sb.append("User: ").append(message.content());
                case ASSISTANT -> {
                    sb.append("Assistant: ").append(message.content());
                    for (ModelToolCall toolCall : message.toolCalls()) {
                        sb.append("\n<tool_call name=\"").append(escapeAttribute(toolCall.name())).append("\">")
                                .append(toolCall.arguments() == null ? "{}" : toolCall.arguments())
                                .append("</tool_call>");
                    }
                }
                case TOOL -> {
                    sb.append("Tool result");
                    if (message.name() != null && !message.name().isBlank()) {
                        sb.append(" (").append(message.name()).append(")");
                    }
                    if (message.toolCallId() != null && !message.toolCallId().isBlank()) {
                        sb.append(" [").append(message.toolCallId()).append("]");
                    }
                    sb.append(": ").append(message.content());
                }
            }
        }
        return sb.toString().trim();
    }

    private static String escapeAttribute(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
