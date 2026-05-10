package org.wrj.haifa.ai.spring.mcp.server.model;

import java.math.BigDecimal;
import java.util.List;

public record CatalogItem(
        String id,
        String name,
        String category,
        BigDecimal price,
        List<String> tags,
        String highlight) {
}
