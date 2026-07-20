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
    void requiresProxyUrlOnlyForProvidersThatEnableProxy() {
        UtilityMcpProperties.Proxy proxy = new UtilityMcpProperties.Proxy();
        UtilityMcpProperties.Provider direct = new UtilityMcpProperties.Provider("https://example.com");
        assertThat(UtilityNetworkProxyConfiguration.proxySettings("direct", proxy, direct)).isNull();

        direct.setProxyEnabled(true);
        assertThatIllegalArgumentException()
                .isThrownBy(() -> UtilityNetworkProxyConfiguration.proxySettings("proxied", proxy, direct))
                .withMessageContaining("proxied")
                .withMessageContaining("UTILITY_MCP_PROXY_URL");
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
