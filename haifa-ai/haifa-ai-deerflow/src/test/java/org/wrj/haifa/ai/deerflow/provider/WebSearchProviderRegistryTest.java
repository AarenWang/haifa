package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSearchProviderRegistryTest {

    private final DuckDuckGoSearchProvider duckDuckGo = new DuckDuckGoSearchProvider();

    @Test
    void resolvesDefaultProvider() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebSearchProvider provider = registry.defaultProvider();
        assertThat(provider.type()).isEqualTo(WebSearchProviderType.DUCKDUCKGO);
    }

    @Test
    void resolvesById() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        WebSearchProvider provider = registry.resolve("duckduckgo");
        assertThat(provider.type()).isEqualTo(WebSearchProviderType.DUCKDUCKGO);
    }

    @Test
    void resolveUnknownIdThrows() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        assertThatThrownBy(() -> registry.resolve("tavily"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No WebSearchProvider registered");
    }

    @Test
    void hasProviderReturnsCorrectly() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        assertThat(registry.hasProvider(WebSearchProviderType.DUCKDUCKGO)).isTrue();
        assertThat(registry.hasProvider(WebSearchProviderType.TAVILY)).isFalse();
    }

    @Test
    void allProvidersReturnsRegistered() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(duckDuckGo));
        assertThat(registry.allProviders()).hasSize(1);
    }
}
