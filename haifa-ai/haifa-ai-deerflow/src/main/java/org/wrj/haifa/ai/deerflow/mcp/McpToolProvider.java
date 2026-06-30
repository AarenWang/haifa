package org.wrj.haifa.ai.deerflow.mcp;

import java.util.List;
import org.wrj.haifa.ai.deerflow.tool.ToolDescriptor;

public class McpToolProvider implements McpClientAdapter {

    private final McpClientAdapter adapter;

    public McpToolProvider(McpClientAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public boolean isEnabled() {
        return adapter.isEnabled();
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return adapter.listTools();
    }

    @Override
    public String executeTool(String toolName, String argumentsJson) {
        return adapter.executeTool(toolName, argumentsJson);
    }
}
