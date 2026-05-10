package org.wrj.haifa.ai.spring.mcp.server.service;

import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.spring.mcp.server.model.CatalogItem;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Component
public class CatalogInventory {

    private final Map<String, CatalogItem> items = new LinkedHashMap<>();

    public CatalogInventory() {
        register(new CatalogItem(
                "sku-1001",
                "Spring AI MCP Starter Kit",
                "developer-tools",
                new BigDecimal("199.00"),
                List.of("mcp", "spring-ai", "starter", "demo"),
                "Starter package for first-time MCP client/server prototypes."));
        register(new CatalogItem(
                "sku-1002",
                "AI Commerce Playbook",
                "knowledge-pack",
                new BigDecimal("69.00"),
                List.of("prompt", "sales", "playbook", "assistant"),
                "Useful for sales prompts, FAQ replies, and recommendation copy."));
        register(new CatalogItem(
                "sku-1003",
                "Vector Search Sandbox",
                "lab",
                new BigDecimal("299.00"),
                List.of("rag", "vector", "search", "retrieval"),
                "Designed for RAG, semantic retrieval, and knowledge search experiments."));
        register(new CatalogItem(
                "sku-1004",
                "Agent Workflow Blueprint",
                "architecture",
                new BigDecimal("159.00"),
                List.of("agent", "workflow", "task", "planning"),
                "A good fit for multi-agent orchestration and tool integration flows."));
    }

    public List<CatalogItem> featuredItems() {
        return items.values().stream()
                .limit(3)
                .toList();
    }

    public List<CatalogItem> search(String scenario, int limit) {
        String normalized = Objects.requireNonNullElse(scenario, "")
                .toLowerCase(Locale.ROOT);

        return items.values().stream()
                .filter(item -> matches(normalized, item))
                .limit(limit)
                .toList();
    }

    public Map<String, Object> onboardingPlaybook() {
        return Map.of(
                "summary", "MCP quick-start checklist",
                "steps", List.of(
                        "Define which tools, resources, and prompts the server should expose.",
                        "Choose whether the client uses STDIO or SSE transport.",
                        "Document discovery, invocation, error handling, and run steps together."));
    }

    private boolean matches(String scenario, CatalogItem item) {
        if (normalizedBlank(scenario)) {
            return true;
        }
        if (item.name().toLowerCase(Locale.ROOT).contains(scenario)) {
            return true;
        }
        if (item.category().toLowerCase(Locale.ROOT).contains(scenario)) {
            return true;
        }
        if (item.highlight().toLowerCase(Locale.ROOT).contains(scenario)) {
            return true;
        }
        return item.tags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .anyMatch(tag -> tag.contains(scenario));
    }

    private boolean normalizedBlank(String value) {
        return value == null || value.isBlank();
    }

    private void register(CatalogItem item) {
        items.put(item.id(), item);
    }
}
