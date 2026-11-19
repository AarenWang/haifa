package org.wrj.haifa.ai.deerflow.mcp;

import java.util.List;
import org.wrj.haifa.ai.deerflow.tool.ToolDescriptor;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

public interface McpClientAdapter {

    boolean isEnabled();

    List<ToolDescriptor> listTools();

    String executeTool(String toolName, String argumentsJson);

    default ToolResult executeToolResult(String toolName, String argumentsJson) {
        return ToolResult.success(toolName, executeTool(toolName, argumentsJson), java.util.Map.of("source", "mcp"));
    }
}
