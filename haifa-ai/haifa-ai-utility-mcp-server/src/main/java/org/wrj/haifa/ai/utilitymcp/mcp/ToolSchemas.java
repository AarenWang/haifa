package org.wrj.haifa.ai.utilitymcp.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ToolSchemas {

    private ToolSchemas() {}

    public static McpSchema.JsonSchema object(Map<String, Object> properties, String... required) {
        return new McpSchema.JsonSchema("object", properties, List.of(required), false, null, null);
    }

    public static Map<String, Object> string(String description, int maxLength) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        schema.put("minLength", 1);
        schema.put("maxLength", maxLength);
        schema.put("description", description);
        return schema;
    }

    public static Map<String, Object> enumeration(String description, String... values) {
        Map<String, Object> schema = new LinkedHashMap<>(string(description, 64));
        schema.put("enum", List.of(values));
        return schema;
    }

    public static Map<String, Object> integer(String description, int minimum, int maximum, int defaultValue) {
        return Map.of("type", "integer", "minimum", minimum, "maximum", maximum,
                "default", defaultValue, "description", description);
    }

    public static Map<String, Object> number(String description, double minimum, double maximum) {
        return Map.of("type", "number", "minimum", minimum, "maximum", maximum, "description", description);
    }

    public static Map<String, Object> outputEnvelope() {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("data", Map.of("type", "object"));
        properties.put("meta", Map.of("type", "object"));
        return Map.of(
                "type", "object",
                "properties", properties,
                "required", new ArrayList<>(List.of("data", "meta")),
                "additionalProperties", false);
    }

    public static McpSchema.Tool tool(
            String name,
            String title,
            String description,
            McpSchema.JsonSchema input,
            boolean openWorld) {
        return McpSchema.Tool.builder()
                .name(name)
                .title(title)
                .description(description)
                .inputSchema(input)
                .outputSchema(outputEnvelope())
                .annotations(new McpSchema.ToolAnnotations(title, true, false, true, openWorld, false))
                .meta(Map.of("contractVersion", "v1"))
                .build();
    }
}
