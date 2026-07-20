package org.wrj.haifa.ai.utilitymcp.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;
import java.util.function.Function;

public record SimpleUtilityTool(
        McpSchema.Tool contract,
        Function<Map<String, Object>, UtilityResult> handler) implements UtilityTool {

    @Override
    public UtilityResult execute(Map<String, Object> arguments) {
        return handler.apply(arguments == null ? Map.of() : arguments);
    }
}
