package org.wrj.haifa.ai.deerflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.genai.types.ProxyType;
import org.junit.jupiter.api.Test;

class GoogleGenAiNetworkProxyConfigurationTest {

    @Test
    void mapsConfiguredSchemesToGoogleSdkProxyTypes() {
        assertThat(GoogleGenAiNetworkProxyConfiguration.proxyTypeFor("http"))
                .isEqualTo(ProxyType.Known.HTTP);
        assertThat(GoogleGenAiNetworkProxyConfiguration.proxyTypeFor("https"))
                .isEqualTo(ProxyType.Known.HTTP);
        assertThat(GoogleGenAiNetworkProxyConfiguration.proxyTypeFor("socks5"))
                .isEqualTo(ProxyType.Known.SOCKS);
    }
}
