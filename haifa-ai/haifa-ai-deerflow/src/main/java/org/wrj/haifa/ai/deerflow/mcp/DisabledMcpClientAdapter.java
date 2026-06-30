package org.wrj.haifa.ai.deerflow.mcp;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.tool.ToolDescriptor;

@Component
public class DisabledMcpClientAdapter implements McpClientAdapter {

    private final boolean enabled;

    public DisabledMcpClientAdapter(org.wrj.haifa.ai.deerflow.config.DeerFlowProperties properties) {
        this.enabled = properties.isMcpEnabled();
    }

    public DisabledMcpClientAdapter() {
        this.enabled = false;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<ToolDescriptor> listTools() {
        return List.of();
    }

    @Override
    public String executeTool(String toolName, String argumentsJson) {
        throw new IllegalStateException("MCP is disabled. Enable it in configuration to use MCP tools.");
    }
}
