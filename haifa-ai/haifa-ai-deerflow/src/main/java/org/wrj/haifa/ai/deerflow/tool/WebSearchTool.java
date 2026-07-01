package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;

/**
 * Unified web search tool.
 *
 * <p>The model-facing tool name is always {@code web_search}. The actual provider
 * (DuckDuckGo, Tavily, Brave, etc.) is determined by configuration and resolved at
 * runtime via {@link WebSearchProviderRegistry}.</p>
 */
@Component
public class WebSearchTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebSearchProviderRegistry registry;
    private final DeerFlowProperties properties;

    public WebSearchTool(WebSearchProviderRegistry registry, DeerFlowProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "web_search";
    }

    @Override
    public String description() {
        String providerId = properties.getWebSearchProvider();
        return "Search the web for queries to find relevant sources and snippets. "
                + "Uses the configured search provider (default: " + org.wrj.haifa.ai.deerflow.provider.WebSearchProviderType.defaultType().displayName() + "). "
                + "Current provider: " + providerId + ". "
                + "Arguments: {\"query\": \"search query\", \"max_results\": 5}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("web_search");
    }

    @Override
    public ToolResult execute(ToolRequest request) {
        String jsonInput = request.userMessage();
        if (jsonInput == null || jsonInput.isBlank()) {
            return ToolResult.of(name(), "Error: arguments JSON required");
        }

        JsonNode node;
        try {
            node = MAPPER.readTree(jsonInput);
        } catch (Exception jsonEx) {
            return ToolResult.of(name(), "Error parsing tool arguments as JSON: " + jsonEx.getMessage());
        }

        String query = node.has("query") ? node.get("query").asText() : null;
        if (query == null || query.isBlank()) {
            return ToolResult.of(name(), "Error: query is required");
        }

        int maxResults = node.has("max_results") ? node.get("max_results").asInt(5) : 5;

        String providerId = properties.getWebSearchProvider();
        WebSearchProvider provider;
        try {
            provider = registry.resolve(providerId);
        } catch (IllegalArgumentException ex) {
            return ToolResult.of(name(), "Error: " + ex.getMessage());
        }

        String result = provider.search(query, maxResults);
        return ToolResult.of(name(), result,
                java.util.Map.of("query", query, "provider", providerId, "maxResults", maxResults));
    }
}
