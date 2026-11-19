package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSearchProviderRegistryTest {

    private final AliyunSearchProvider aliyun = new AliyunSearchProvider();
    private final DuckDuckGoSearchProvider duckDuckGo = new DuckDuckGoSearchProvider();

    @Test
    void resolvesDefaultProvider() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(aliyun, duckDuckGo));
        WebSearchProvider provider = registry.defaultProvider();
        assertThat(provider.type()).isEqualTo(WebSearchProviderType.ALIYUN);
    }

    @Test
    void resolvesById() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(aliyun, duckDuckGo));
        WebSearchProvider provider = registry.resolve("aliyun");
        assertThat(provider.type()).isEqualTo(WebSearchProviderType.ALIYUN);
    }

    @Test
    void resolveUnknownIdThrows() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(aliyun, duckDuckGo));
        assertThatThrownBy(() -> registry.resolve("tavily"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No WebSearchProvider registered");
    }

    @Test
    void hasProviderReturnsCorrectly() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(aliyun, duckDuckGo));
        assertThat(registry.hasProvider(WebSearchProviderType.ALIYUN)).isTrue();
        assertThat(registry.hasProvider(WebSearchProviderType.TAVILY)).isFalse();
    }

    @Test
    void allProvidersReturnsRegistered() {
        WebSearchProviderRegistry registry = new WebSearchProviderRegistry(List.of(aliyun, duckDuckGo));
        assertThat(registry.allProviders()).hasSize(2);
    }
}
