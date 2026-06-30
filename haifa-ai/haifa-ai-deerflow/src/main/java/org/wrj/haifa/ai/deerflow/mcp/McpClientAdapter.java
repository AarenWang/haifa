package org.wrj.haifa.ai.deerflow.mcp;

import java.util.List;
import org.wrj.haifa.ai.deerflow.tool.ToolDescriptor;

public interface McpClientAdapter {

    boolean isEnabled();

    List<ToolDescriptor> listTools();

    String executeTool(String toolName, String argumentsJson);
}
