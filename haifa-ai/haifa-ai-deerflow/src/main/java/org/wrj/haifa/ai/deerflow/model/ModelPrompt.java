package org.wrj.haifa.ai.deerflow.model;

import java.util.List;
import org.wrj.haifa.ai.deerflow.model.cache.PromptBlock;
import org.wrj.haifa.ai.deerflow.model.cache.PromptCacheContext;

public record ModelPrompt(
    String systemPrompt,
    String userPrompt,
    String modelName,
    List<ModelMessage> messages,
    List<ModelToolDefinition> toolDefinitions,
    List<PromptBlock> promptBlocks,
    PromptCacheContext cacheContext
) {
    public ModelPrompt {
        systemPrompt = systemPrompt == null ? "" : systemPrompt;
        userPrompt = userPrompt == null ? "" : userPrompt;
        messages = messages == null ? List.of() : List.copyOf(messages);
        toolDefinitions = toolDefinitions == null ? List.of() : List.copyOf(toolDefinitions);
        promptBlocks = promptBlocks == null ? List.of() : List.copyOf(promptBlocks);
        cacheContext = cacheContext == null ? PromptCacheContext.disabled() : cacheContext;
    }

    public ModelPrompt(String systemPrompt, String userPrompt, String modelName, List<ModelMessage> messages, List<ModelToolDefinition> toolDefinitions) {
        this(systemPrompt, userPrompt, modelName, messages, toolDefinitions, List.of(), PromptCacheContext.disabled());
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

    public ModelPrompt withSystemPrompt(String newSystemPrompt) {
        return new ModelPrompt(newSystemPrompt, userPrompt, modelName, messages, toolDefinitions, promptBlocks, cacheContext);
    }

    public ModelPrompt withUserPrompt(String newUserPrompt) {
        return new ModelPrompt(systemPrompt, newUserPrompt, modelName, messages, toolDefinitions, promptBlocks, cacheContext);
    }

    public ModelPrompt withModelName(String newModelName) {
        return new ModelPrompt(systemPrompt, userPrompt, newModelName, messages, toolDefinitions, promptBlocks, cacheContext);
    }


    public ModelPrompt withMessages(List<ModelMessage> newMessages) {
        return new ModelPrompt(systemPrompt, userPrompt, modelName, newMessages, toolDefinitions, promptBlocks, cacheContext);
    }

    public ModelPrompt withToolDefinitions(List<ModelToolDefinition> newToolDefinitions) {
        return new ModelPrompt(systemPrompt, userPrompt, modelName, messages, newToolDefinitions, promptBlocks, cacheContext);
    }

    public ModelPrompt withPromptBlocks(List<PromptBlock> newPromptBlocks) {
        return new ModelPrompt(systemPrompt, userPrompt, modelName, messages, toolDefinitions, newPromptBlocks, cacheContext);
    }

    public ModelPrompt withCacheContext(PromptCacheContext newCacheContext) {
        return new ModelPrompt(systemPrompt, userPrompt, modelName, messages, toolDefinitions, promptBlocks, newCacheContext);
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
                        sb.append("\nStructured tool call requested")
                                .append(" [id=").append(toolCall.id() == null ? "" : toolCall.id()).append("]")
                                .append(" [name=").append(toolCall.name() == null ? "" : toolCall.name()).append("]");
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
}
