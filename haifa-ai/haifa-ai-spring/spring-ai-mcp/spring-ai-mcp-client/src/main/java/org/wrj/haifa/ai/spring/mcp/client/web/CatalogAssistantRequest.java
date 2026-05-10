package org.wrj.haifa.ai.spring.mcp.client.web;

public record CatalogAssistantRequest(
        String scenario,
        Integer limit,
        String audience,
        String goal) {
}
