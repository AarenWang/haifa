package org.wrj.haifa.ai.deerflow.mcp;

import java.util.List;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.tool.AgentTool;

@Component
public class McpDynamicToolProvider {

    private final McpConnectionManager manager;

    public McpDynamicToolProvider(McpConnectionManager manager) {
        this.manager = manager;
    }

    public List<AgentTool> tools() {
        return manager.snapshot().toolsByExposedName().values().stream()
                .sorted(java.util.Comparator.comparing(McpToolIdentity::exposedName))
                .map(identity -> (AgentTool) new McpAgentTool(identity, manager))
                .toList();
    }
}
