package org.wrj.haifa.ai.deerflow.tool;

public interface AgentTool {

    String name();

    String description();

    boolean supports(String userMessage);

    ToolResult execute(ToolRequest request);
}
