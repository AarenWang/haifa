package org.wrj.haifa.ai.spring.mcp.server.model;

import java.util.List;

public record CatalogSearchResult(
        String scenario,
        int total,
        List<CatalogItem> items) {
}
