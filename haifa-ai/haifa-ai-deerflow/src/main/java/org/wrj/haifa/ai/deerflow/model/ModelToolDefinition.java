package org.wrj.haifa.ai.deerflow.model;

/**
 * Model-facing structured tool definition.
 */
public record ModelToolDefinition(
        String name,
        String description,
        String inputSchema
) {
    private static final String DEFAULT_INPUT_SCHEMA = """
            {
              "type": "object",
              "properties": {},
              "additionalProperties": true
            }
            """;

    public ModelToolDefinition {
        name = name == null ? "" : name.trim();
        description = description == null ? "" : description.trim();
        inputSchema = inputSchema == null || inputSchema.isBlank() ? DEFAULT_INPUT_SCHEMA : inputSchema.trim();
    }
}
