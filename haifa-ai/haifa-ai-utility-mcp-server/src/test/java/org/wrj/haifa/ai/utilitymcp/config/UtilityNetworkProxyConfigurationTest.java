package org.wrj.haifa.ai.utilitymcp.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.junit.jupiter.api.Test;

class UtilityNetworkProxyConfigurationTest {

    @Test
    void parsesHttpAndAuthenticatedSocks5ProxySettings() {
        assertThat(UtilityNetworkProxyConfiguration.parseProxySettings(
                "http://proxy.internal", null, null))
                .extracting("scheme", "host", "port", "proxyType", "username", "password")
                .containsExactly("http", "proxy.internal", 80,
                        reactor.netty.transport.ProxyProvider.Proxy.HTTP, null, null);

        assertThat(UtilityNetworkProxyConfiguration.parseProxySettings(
                "socks5://127.0.0.1:1081", "proxy-user", "proxy-password"))
                .extracting("scheme", "host", "port", "proxyType", "username", "password")
                .containsExactly("socks5", "127.0.0.1", 1081,
                        reactor.netty.transport.ProxyProvider.Proxy.SOCKS5,
                        "proxy-user", "proxy-password");
    }

    @Test
    void selectsProvidersFromCommaSeparatedListAndRequiresProxyUrl() {
        UtilityMcpProperties.Proxy proxy = new UtilityMcpProperties.Proxy();
        proxy.setProviders(" wikimedia, OPEN-METEO ");
        assertThat(UtilityNetworkProxyConfiguration.selectedProviders(proxy.getProviders()))
                .containsExactly("wikimedia", "open-meteo");
        assertThat(UtilityNetworkProxyConfiguration.proxySettings("frankfurter", proxy)).isNull();

        assertThatIllegalArgumentException()
                .isThrownBy(() -> UtilityNetworkProxyConfiguration.proxySettings("wikimedia", proxy))
                .withMessageContaining("wikimedia")
                .withMessageContaining("UTILITY_MCP_PROXY_URL");

        proxy.setUrl("http://127.0.0.1:7890");
        assertThat(UtilityNetworkProxyConfiguration.proxySettings("wikimedia", proxy)).isNotNull();
        assertThat(UtilityNetworkProxyConfiguration.proxySettings("open-meteo", proxy)).isNotNull();
    }

    @Test
    void rejectsUnknownProviderInProxyList() {
        UtilityMcpProperties.Proxy proxy = new UtilityMcpProperties.Proxy();
        proxy.setProviders("wikimedia,wikimeda");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> UtilityNetworkProxyConfiguration.proxySettings("wikimedia", proxy))
                .withMessageContaining("wikimeda")
                .withMessageContaining("Supported providers");
    }

    @Test
    void rejectsEmbeddedCredentialsAndPasswordWithoutUsername() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UtilityNetworkProxyConfiguration.parseProxySettings(
                        "http://user:secret@127.0.0.1:7890", null, null))
                .withMessageContaining("Expected http://, https://, or socks5://");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UtilityNetworkProxyConfiguration.parseProxySettings(
                        "http://127.0.0.1:7890", null, "proxy-password"))
                .withMessageContaining("UTILITY_MCP_PROXY_USERNAME is required");
    }
}
