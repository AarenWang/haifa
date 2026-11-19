package org.wrj.haifa.ai.deerflow.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebSearchProviderTypeTest {

    @Test
    void defaultTypeIsAliyun() {
        assertThat(WebSearchProviderType.defaultType()).isEqualTo(WebSearchProviderType.ALIYUN);
    }

    @Test
    void fromIdReturnsCorrectType() {
        assertThat(WebSearchProviderType.fromId("duckduckgo")).isEqualTo(WebSearchProviderType.DUCKDUCKGO);
        assertThat(WebSearchProviderType.fromId("DuckDuckGo")).isEqualTo(WebSearchProviderType.DUCKDUCKGO);
        assertThat(WebSearchProviderType.fromId("tavily")).isEqualTo(WebSearchProviderType.TAVILY);
        assertThat(WebSearchProviderType.fromId("brave")).isEqualTo(WebSearchProviderType.BRAVE);
    }

    @Test
    void fromIdNullOrBlankReturnsDefault() {
        assertThat(WebSearchProviderType.fromId(null)).isEqualTo(WebSearchProviderType.defaultType());
        assertThat(WebSearchProviderType.fromId("")).isEqualTo(WebSearchProviderType.defaultType());
        assertThat(WebSearchProviderType.fromId("   ")).isEqualTo(WebSearchProviderType.defaultType());
    }

    @Test
    void fromIdUnknownThrows() {
        assertThatThrownBy(() -> WebSearchProviderType.fromId("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown web_search provider")
                .hasMessageContaining("unknown");
    }

    @Test
    void metadataIsCorrect() {
        ProviderMetadata meta = WebSearchProviderType.ALIYUN.toMetadata();
        assertThat(meta.id()).isEqualTo("aliyun");
        assertThat(meta.displayName()).isEqualTo("Aliyun IQS Search");
        assertThat(meta.requiresApiKey()).isTrue();
        assertThat(meta.defaultEnabled()).isTrue();
        assertThat(meta.supportsSearch()).isTrue();
        assertThat(meta.supportsFetch()).isFalse();
    }

    @Test
    void allIdsContainsExpectedValues() {
        assertThat(WebSearchProviderType.allIds()).contains("duckduckgo", "tavily", "brave", "exa", "firecrawl", "infoquest", "groundroute", "serper", "searxng", "fastcrw");
    }
}
