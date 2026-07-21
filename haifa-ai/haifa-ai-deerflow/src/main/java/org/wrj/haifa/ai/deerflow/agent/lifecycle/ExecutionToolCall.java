package org.wrj.haifa.ai.deerflow.agent.lifecycle;

import java.util.Map;

public record ExecutionToolCall(String id, String toolName, String arguments, Map<String, Object> metadata) {
    public ExecutionToolCall {
        id = id == null ? "" : id;
        toolName = toolName == null ? "" : toolName;
        arguments = arguments == null ? "" : arguments;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
