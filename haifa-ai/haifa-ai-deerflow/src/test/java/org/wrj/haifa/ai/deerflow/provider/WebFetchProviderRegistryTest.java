package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebFetchProviderRegistryTest {

    private final AliyunFetchProvider aliyun = new AliyunFetchProvider();
    private final JinaAiFetchProvider jina = new JinaAiFetchProvider();

    @Test
    void resolvesDefaultProvider() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(aliyun, jina));
        WebFetchProvider provider = registry.defaultProvider();
        assertThat(provider.type()).isEqualTo(WebFetchProviderType.ALIYUN);
    }

    @Test
    void resolvesById() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(aliyun, jina));
        WebFetchProvider provider = registry.resolve("aliyun");
        assertThat(provider.type()).isEqualTo(WebFetchProviderType.ALIYUN);
    }

    @Test
    void resolveUnknownIdThrows() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(aliyun, jina));
        assertThatThrownBy(() -> registry.resolve("exa"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No WebFetchProvider registered");
    }

    @Test
    void hasProviderReturnsCorrectly() {
        WebFetchProviderRegistry registry = new WebFetchProviderRegistry(List.of(aliyun, jina));
        assertThat(registry.hasProvider(WebFetchProviderType.ALIYUN)).isTrue();
        assertThat(registry.hasProvider(WebFetchProviderType.EXA)).isFalse();
    }
}
