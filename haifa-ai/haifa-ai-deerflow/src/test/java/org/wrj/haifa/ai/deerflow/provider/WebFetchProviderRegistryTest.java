package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebFetchProviderRegistryTest {

    private final JinaAiFetchProvider jina = new JinaAiFetchProvider();

    @Test
    void resolvesDefaultProvider() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(jina));
        WebFetchProvider provider = registry.defaultProvider();
        assertThat(provider.type()).isEqualTo(WebFetchProviderType.JINA);
    }

    @Test
    void resolvesById() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(jina));
        WebFetchProvider provider = registry.resolve("jina");
        assertThat(provider.type()).isEqualTo(WebFetchProviderType.JINA);
    }

    @Test
    void resolveUnknownIdThrows() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(jina));
        assertThatThrownBy(() -> registry.resolve("exa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No WebFetchProvider registered");
    }

    @Test
    void hasProviderReturnsCorrectly() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(jina));
        assertThat(registry.hasProvider(WebFetchProviderType.JINA)).isTrue();
        assertThat(registry.hasProvider(WebFetchProviderType.EXA)).isFalse();
    }
}
