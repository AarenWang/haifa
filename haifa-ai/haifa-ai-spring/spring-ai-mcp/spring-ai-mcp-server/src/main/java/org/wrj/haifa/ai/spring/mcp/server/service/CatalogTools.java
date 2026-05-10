package org.wrj.haifa.ai.spring.mcp.server.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;
import org.wrj.haifa.ai.spring.mcp.server.model.CatalogSearchResult;

@Service
public class CatalogTools {

    private final CatalogInventory catalogInventory;

    public CatalogTools(CatalogInventory catalogInventory) {
        this.catalogInventory = catalogInventory;
    }

    @Tool(name = "searchCatalog", description = "Search the demo product catalog for MCP scenarios")
    public CatalogSearchResult searchCatalog(
            @ToolParam(description = "Scenario, keyword, or user request", required = true) String scenario,
            @ToolParam(description = "Maximum number of items, default is 3", required = false) Integer limit) {

        int effectiveLimit = limit == null || limit < 1 ? 3 : Math.min(limit, 5);
        return new CatalogSearchResult(
                scenario,
                effectiveLimit,
                catalogInventory.search(scenario, effectiveLimit));
    }
}
