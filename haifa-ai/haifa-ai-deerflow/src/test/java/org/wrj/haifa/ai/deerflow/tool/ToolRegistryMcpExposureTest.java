package org.wrj.haifa.ai.deerflow.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.mcp.McpToolSelectionStore;

class ToolRegistryMcpExposureTest {

    @Test
    void largeCatalogIsRunIsolatedUntilToolSearchSelectsTools() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getMcp().setDirectExposureThreshold(1);
        McpToolSelectionStore selections = new McpToolSelectionStore();
        ToolRegistry registry = new ToolRegistry(List.of(tool("core"), tool("mcp__a__one"), tool("mcp__a__two")),
                properties, selections);

        assertThat(names(registry.modelVisibleTools("run-a"))).containsExactly("core");
        selections.select("run-a", List.of("mcp__a__two"));
        assertThat(names(registry.modelVisibleTools("run-a"))).containsExactly("core", "mcp__a__two");
        assertThat(names(registry.modelVisibleTools("run-b"))).containsExactly("core");
    }

    private static AgentTool tool(String name) {
        return new AgentTool() {
            @Override public String name() { return name; }
            @Override public String description() { return name; }
            @Override public boolean supports(String userMessage) { return true; }
            @Override public ToolResult execute(ToolRequest request) { return ToolResult.of(name, "ok"); }
        };
    }

    private static List<String> names(List<AgentTool> tools) {
        return tools.stream().map(AgentTool::name).toList();
    }
}
