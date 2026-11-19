package org.wrj.haifa.ai.deerflow.provider;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebFetchProviderTypeTest {

    @Test
    void defaultTypeIsAliyun() {
        assertThat(WebFetchProviderType.defaultType()).isEqualTo(WebFetchProviderType.ALIYUN);
    }

    @Test
    void fromIdReturnsCorrectType() {
        assertThat(WebFetchProviderType.fromId("jina")).isEqualTo(WebFetchProviderType.JINA);
        assertThat(WebFetchProviderType.fromId("JINA")).isEqualTo(WebFetchProviderType.JINA);
        assertThat(WebFetchProviderType.fromId("exa")).isEqualTo(WebFetchProviderType.EXA);
        assertThat(WebFetchProviderType.fromId("firecrawl")).isEqualTo(WebFetchProviderType.FIRECRAWL);
    }

    @Test
    void fromIdNullOrBlankReturnsDefault() {
        assertThat(WebFetchProviderType.fromId(null)).isEqualTo(WebFetchProviderType.defaultType());
        assertThat(WebFetchProviderType.fromId("")).isEqualTo(WebFetchProviderType.defaultType());
    }

    @Test
    void fromIdUnknownThrows() {
        assertThatThrownBy(() -> WebFetchProviderType.fromId("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown web_fetch provider")
                .hasMessageContaining("unknown");
    }

    @Test
    void metadataIsCorrect() {
        ProviderMetadata meta = WebFetchProviderType.ALIYUN.toMetadata();
        assertThat(meta.id()).isEqualTo("aliyun");
        assertThat(meta.displayName()).isEqualTo("Aliyun IQS Fetch");
        assertThat(meta.requiresApiKey()).isTrue();
        assertThat(meta.defaultEnabled()).isTrue();
        assertThat(meta.supportsSearch()).isFalse();
        assertThat(meta.supportsFetch()).isTrue();
    }

    @Test
    void allIdsContainsExpectedValues() {
        assertThat(WebFetchProviderType.allIds()).contains("jina", "exa", "firecrawl", "infoquest", "groundroute", "browserless", "fastcrw");
    }
}
