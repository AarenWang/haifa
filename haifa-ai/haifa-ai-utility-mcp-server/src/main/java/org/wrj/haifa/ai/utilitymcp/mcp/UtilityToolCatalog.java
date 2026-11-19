package org.wrj.haifa.ai.utilitymcp.mcp;

import java.util.List;

public record UtilityToolCatalog(List<UtilityTool> tools) {
    public UtilityToolCatalog {
        tools = List.copyOf(tools);
    }
}
