package org.wrj.haifa.ai.deerflow.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;

/**
 * Unified web fetch tool.
 *
 * <p>The model-facing tool name is always {@code web_fetch}. The actual provider
 * (Jina AI, Exa, Firecrawl, etc.) is determined by configuration and resolved at
 * runtime via {@link WebFetchProviderRegistry}.</p>
 */
@Component
public class WebFetchTool implements AgentTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final WebFetchProviderRegistry registry;
    private final DeerFlowProperties properties;

    public WebFetchTool(WebFetchProviderRegistry registry, DeerFlowProperties properties) {
        this.registry = registry;
        this.properties = properties;
    }

    @Override
    public String name() {
        return "web_fetch";
    }

    @Override
    public String description() {
        String providerId = properties.getWebFetchProvider();
        return "Fetch and read the full markdown content of a URL. "
                + "Uses the configured fetch provider (default: " + org.wrj.haifa.ai.deerflow.provider.WebFetchProviderType.defaultType().displayName() + "). "
                + "Current provider: " + providerId + ". "
                + "Arguments: {\"url\": \"https://example.com\"}";
    }

    @Override
    public boolean supports(String userMessage) {
        return userMessage != null && userMessage.toLowerCase().contains("web_fetch");
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

        String url = node.has("url") ? node.get("url").asText() : null;
        if (url == null || url.isBlank()) {
            return ToolResult.of(name(), "Error: url is required");
        }

        String providerId = properties.getWebFetchProvider();
        WebFetchProvider provider;
        try {
            provider = registry.resolve(providerId);
        } catch (IllegalArgumentException ex) {
            return ToolResult.of(name(), "Error: " + ex.getMessage());
        }

        String result = provider.fetch(url);
        return ToolResult.of(name(), result,
                java.util.Map.of("url", url, "provider", providerId));
    }
}
