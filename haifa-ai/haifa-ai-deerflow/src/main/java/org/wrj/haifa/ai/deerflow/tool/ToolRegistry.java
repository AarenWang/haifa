package org.wrj.haifa.ai.deerflow.tool;

import java.util.List;
import java.util.ArrayList;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.mcp.McpDynamicToolProvider;
import org.wrj.haifa.ai.deerflow.mcp.McpToolSelectionStore;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

@Component
public class ToolRegistry {

    private final List<AgentTool> staticTools;
    private final McpDynamicToolProvider mcpTools;
    private final DeerFlowProperties properties;
    private final McpToolSelectionStore selectionStore;

    public ToolRegistry(List<AgentTool> tools) {
        this.staticTools = List.copyOf(tools);
        this.mcpTools = null;
        this.properties = new DeerFlowProperties();
        this.selectionStore = null;
    }

    ToolRegistry(List<AgentTool> tools, DeerFlowProperties properties, McpToolSelectionStore selectionStore) {
        this.staticTools = List.copyOf(tools);
        this.mcpTools = null;
        this.properties = properties;
        this.selectionStore = selectionStore;
    }

    @Autowired
    public ToolRegistry(List<AgentTool> tools, ObjectProvider<McpDynamicToolProvider> mcpTools,
            DeerFlowProperties properties, McpToolSelectionStore selectionStore) {
        this.staticTools = List.copyOf(tools);
        this.mcpTools = mcpTools.getIfAvailable();
        this.properties = properties;
        this.selectionStore = selectionStore;
    }

    public List<AgentTool> plan(String userMessage, int maxTools) {
        return tools().stream()
                .filter(tool -> tool.supports(userMessage))
                .limit(Math.max(0, maxTools))
                .toList();
    }

    public List<AgentTool> tools() {
        if (mcpTools == null) return staticTools;
        List<AgentTool> result = new ArrayList<>(staticTools);
        result.addAll(mcpTools.tools());
        return List.copyOf(result);
    }

    public List<AgentTool> modelVisibleTools(String runId) {
        List<AgentTool> all = tools();
        long mcpCount = all.stream().filter(tool -> tool.name().startsWith("mcp__")).count();
        if (mcpCount <= properties.getMcp().getDirectExposureThreshold()) return all;
        java.util.Set<String> selected = selectionStore == null ? java.util.Set.of() : selectionStore.selected(runId);
        return all.stream().filter(tool -> !tool.name().startsWith("mcp__") || selected.contains(tool.name())).toList();
    }
}
