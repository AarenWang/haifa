package org.wrj.haifa.ai.spring.mcp.client.web;

public record CatalogSearchRequest(
        String scenario,
        Integer limit) {
}
