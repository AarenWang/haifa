package org.wrj.haifa.ai.deerflow.tool;

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;
import org.wrj.haifa.ai.deerflow.provider.DuckDuckGoSearchProvider;
import org.wrj.haifa.ai.deerflow.provider.JinaAiFetchProvider;
import org.wrj.haifa.ai.deerflow.provider.WebFetchProviderRegistry;
import org.wrj.haifa.ai.deerflow.provider.WebSearchProviderRegistry;

import static org.assertj.core.api.Assertions.assertThat;

class WebSearchFetchToolTest {

    @Test
    void webSearchToolUsesDefaultProvider() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(
                java.util.List.of(new DuckDuckGoSearchProvider()));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(
                java.util.List.of(new JinaAiFetchProvider()));

        WebSearchTool searchTool = new WebSearchTool(searchRegistry, properties);
        WebFetchTool fetchTool = new WebFetchTool(fetchRegistry, properties);

        // Tool names are stable
        assertThat(searchTool.name()).isEqualTo("web_search");
        assertThat(fetchTool.name()).isEqualTo("web_fetch");

        // Default provider reflected in description
        assertThat(searchTool.description()).contains("DuckDuckGo");
        assertThat(searchTool.description()).contains("duckduckgo");
        assertThat(fetchTool.description()).contains("Jina AI Reader");
        assertThat(fetchTool.description()).contains("jina");
    }

    @Test
    void webSearchToolExecutesWithDefaultProvider() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(
                java.util.List.of(new DuckDuckGoSearchProvider()));
        WebSearchTool searchTool = new WebSearchTool(searchRegistry, properties);

        ToolRequest request = new ToolRequest("{\"query\": \"Spring Boot\", \"max_results\": 3}", Path.of("."));
        ToolResult result = searchTool.execute(request);

        assertThat(result.toolName()).isEqualTo("web_search");
        assertThat(result.content()).contains("Spring Boot");
        assertThat(result.metadata()).containsEntry("provider", "duckduckgo");
        assertThat(result.metadata()).containsEntry("maxResults", 3);
    }

    @Test
    void webFetchToolExecutesWithDefaultProvider() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(
                java.util.List.of(new JinaAiFetchProvider()));
        WebFetchTool fetchTool = new WebFetchTool(fetchRegistry, properties);

        ToolRequest request = new ToolRequest("{\"url\": \"https://example.com\"}", Path.of("."));
        ToolResult result = fetchTool.execute(request);

        assertThat(result.toolName()).isEqualTo("web_fetch");
        assertThat(result.content()).contains("https://example.com");
        assertThat(result.metadata()).containsEntry("provider", "jina");
    }

    @Test
    void providerSwitchChangesBehaviorWithoutChangingToolName() {
        // Simulate configuration switch to an unknown provider (enum validates but registry misses)
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getTools().getWebSearch().setProvider("tavily");

        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(
                java.util.List.of(new DuckDuckGoSearchProvider()));
        WebSearchTool searchTool = new WebSearchTool(searchRegistry, properties);

        // Tool name stays the same
        assertThat(searchTool.name()).isEqualTo("web_search");
        // Description reflects the configured provider
        assertThat(searchTool.description()).contains("tavily");

        // Execution fails because Tavily provider is not registered yet
        ToolRequest request = new ToolRequest("{\"query\": \"test\"}", Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("Error:").contains("No WebSearchProvider registered");
    }

    @Test
    void illegalProviderIdProducesClearError() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getTools().getWebFetch().setProvider("not-a-provider");

        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(
                java.util.List.of(new JinaAiFetchProvider()));
        WebFetchTool fetchTool = new WebFetchTool(fetchRegistry, properties);

        ToolRequest request = new ToolRequest("{\"url\": \"https://example.com\"}", Path.of("."));
        ToolResult result = fetchTool.execute(request);
        assertThat(result.content()).contains("Error:").contains("Unknown web_fetch provider");
    }

    @Test
    void webSearchToolRequiresQueryArgument() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(
                java.util.List.of(new DuckDuckGoSearchProvider()));
        WebSearchTool searchTool = new WebSearchTool(searchRegistry, properties);

        ToolRequest request = new ToolRequest("{}", Path.of("."));
        ToolResult result = searchTool.execute(request);
        assertThat(result.content()).contains("query is required");
    }

    @Test
    void webFetchToolRequiresUrlArgument() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(
                java.util.List.of(new JinaAiFetchProvider()));
        WebFetchTool fetchTool = new WebFetchTool(fetchRegistry, properties);

        ToolRequest request = new ToolRequest("{}", Path.of("."));
        ToolResult result = fetchTool.execute(request);
        assertThat(result.content()).contains("url is required");
    }
}
