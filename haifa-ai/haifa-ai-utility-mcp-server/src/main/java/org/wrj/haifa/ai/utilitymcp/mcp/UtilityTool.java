package org.wrj.haifa.ai.utilitymcp.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.Map;

public interface UtilityTool {

    McpSchema.Tool contract();

    UtilityResult execute(Map<String, Object> arguments);
}
