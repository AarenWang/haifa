package org.wrj.haifa.ai.deerflow.mcp;

import org.wrj.haifa.ai.deerflow.tool.AgentTool;
import org.wrj.haifa.ai.deerflow.tool.ToolRequest;
import org.wrj.haifa.ai.deerflow.tool.ToolResult;

public final class McpAgentTool implements AgentTool {

    private final McpToolIdentity identity;
    private final McpConnectionManager manager;

    public McpAgentTool(McpToolIdentity identity, McpConnectionManager manager) {
        this.identity = identity;
        this.manager = manager;
    }

    public McpToolIdentity identity() {
        return identity;
    }

    @Override public String name() { return identity.exposedName(); }
    @Override public String description() { return identity.description(); }
    @Override public String inputSchema() { return identity.inputSchema(); }
    @Override public boolean supports(String userMessage) { return true; }

    @Override
    public ToolResult execute(ToolRequest request) {
        return manager.execute(identity, request.userMessage(), request.threadId(), request.runId());
    }
}
