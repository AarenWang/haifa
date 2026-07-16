package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import org.wrj.haifa.ai.deerflow.completion.ToolCompletionContract;

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

    /**
     * Declares completion requirements and successful evidence types owned by this Tool implementation.
     * The runtime, not model-provided arguments, uses these contracts to build the evidence ledger.
     */
    default List<ToolCompletionContract> completionContracts() {
        return List.of();
    }

    boolean supports(String userMessage);

    ToolResult execute(ToolRequest request);
}
