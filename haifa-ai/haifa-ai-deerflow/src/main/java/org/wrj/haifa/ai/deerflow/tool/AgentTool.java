package org.wrj.haifa.ai.deerflow.tool;

public interface AgentTool {

    String DEFAULT_INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": true
            }
            """;

    String name();

    String description();

    default String inputSchema() {
        return DEFAULT_INPUT_SCHEMA;
    }

    boolean supports(String userMessage);

    ToolResult execute(ToolRequest request);
}
