package org.wrj.haifa.ai.deerflow.agent.lifecycle;

public record ToolCallFilterResult(ExecutionToolCall toolCall, boolean allowed, String reason) { }
