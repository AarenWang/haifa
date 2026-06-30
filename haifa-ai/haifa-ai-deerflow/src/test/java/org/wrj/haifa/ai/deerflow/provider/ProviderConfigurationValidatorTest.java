package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ProviderConfigurationValidatorTest {

    private final DuckDuckGoSearchProvider duckDuckGo = new DuckDuckGoSearchProvider();
    private final JinaAiFetchProvider jina = new JinaAiFetchProvider();

    @Test
    void defaultConfigPassesValidation() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatNoException().isThrownBy(validator::validate);
    }

    @Test
    void unregisteredSearchProviderFailsAtStartup() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getTools().getWebSearch().setProvider("tavily"); // known enum but no bean
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configured web_search provider 'tavily' is not registered")
                .hasMessageContaining("Registered providers")
                .hasMessageContaining("add its implementation bean");
    }

    @Test
    void unknownSearchProviderIdFailsAtStartup() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getTools().getWebSearch().setProvider("not-a-provider");
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unknown web_search provider");
    }

    @Test
    void unregisteredFetchProviderFailsAtStartup() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.getTools().getWebFetch().setProvider("exa"); // known enum but no bean
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Configured web_fetch provider 'exa' is not registered")
                .hasMessageContaining("add its implementation bean");
    }

    @Test
    void requiresApiKeySearchProviderMissingKeyFailsAtStartup() {
        DeerFlowProperties properties = new DeerFlowProperties();
        // We need a provider that requires API key AND is registered
        // For this test, we create a fake provider that requires API key
        WebSearchProvider tavilyProvider = new WebSearchProvider() {
            @Override
            public WebSearchProviderType type() { return WebSearchProviderType.TAVILY; }
            @Override
            public String search(String query, int maxResults) { return "stub"; }
        };
        properties.getTools().getWebSearch().setProvider("tavily");
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(tavilyProvider));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires an API key")
                .hasMessageContaining("haifa.ai.deerflow.tools.web-search.api-key")
                .hasMessageContaining("TAVILY_API_KEY");
    }

    @Test
    void requiresApiKeyFetchProviderMissingKeyFailsAtStartup() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebFetchProvider exaProvider = new WebFetchProvider() {
            @Override
            public WebFetchProviderType type() { return WebFetchProviderType.EXA; }
            @Override
            public String fetch(String url) { return "stub"; }
        };
        properties.getTools().getWebFetch().setProvider("exa");
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(exaProvider));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatThrownBy(validator::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("requires an API key")
                .hasMessageContaining("haifa.ai.deerflow.tools.web-fetch.api-key")
                .hasMessageContaining("EXA_API_KEY");
    }

    @Test
    void requiresApiKeyWithConfiguredKeyPasses() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebSearchProvider tavilyProvider = new WebSearchProvider() {
            @Override
            public WebSearchProviderType type() { return WebSearchProviderType.TAVILY; }
            @Override
            public String search(String query, int maxResults) { return "stub"; }
        };
        properties.getTools().getWebSearch().setProvider("tavily");
        properties.getTools().getWebSearch().setApiKey("sk-test123");
        WebSearchProviderRegistry searchRegistry = new WebSearchProviderRegistry(List.of(tavilyProvider));
        WebFetchProviderRegistry fetchRegistry = new WebFetchProviderRegistry(List.of(jina));

        ProviderConfigurationValidator validator = new ProviderConfigurationValidator(
                properties, searchRegistry, fetchRegistry);

        assertThatNoException().isThrownBy(validator::validate);
    }
}
