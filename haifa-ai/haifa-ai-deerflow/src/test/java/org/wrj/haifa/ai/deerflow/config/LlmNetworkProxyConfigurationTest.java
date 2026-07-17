package org.wrj.haifa.ai.deerflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

class LlmNetworkProxyConfigurationTest {

    private final LlmNetworkProxyConfiguration configuration = new LlmNetworkProxyConfiguration();

    @Test
    void createsBlockingAndStreamingProxyCustomizersWhenConfigured() {
        DeerFlowProperties properties = new DeerFlowProperties();
        properties.setNetworkProxyUrl("http://127.0.0.1:7890");
        properties.setNetworkProxyUsername("proxy-user");
        properties.setNetworkProxyPassword("proxy-password");
        RestClient.Builder restClientBuilder = mock(RestClient.Builder.class);
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);
        LlmNetworkProxyConfiguration.ProxySettings proxy = LlmNetworkProxyConfiguration.parseProxySettings(
                properties.getNetworkProxyUrl(),
                properties.getNetworkProxyUsername(),
                properties.getNetworkProxyPassword());

        assertThat(configuration.llmProxyRestClientBuilder(properties)).isNotNull();
        LlmNetworkProxyConfiguration.configureRestClientBuilder(restClientBuilder, proxy);
        configuration.llmProxyWebClientCustomizer(properties).customize(webClientBuilder);

        verify(restClientBuilder).requestFactory(any());
        verify(webClientBuilder).clientConnector(any());
    }

    @Test
    void leavesHttpClientsUntouchedWhenProxyIsNotConfigured() {
        DeerFlowProperties properties = new DeerFlowProperties();
        WebClient.Builder webClientBuilder = mock(WebClient.Builder.class);

        configuration.llmProxyWebClientCustomizer(properties).customize(webClientBuilder);

        verifyNoInteractions(webClientBuilder);
    }

    @Test
    void parsesProxyUriAndAppliesDefaultPort() {
        assertThat(LlmNetworkProxyConfiguration.parseProxySettings("http://proxy.internal", null, null))
                .extracting("scheme", "host", "port", "proxyType")
                .containsExactly("http", "proxy.internal", 80, reactor.netty.transport.ProxyProvider.Proxy.HTTP);
        assertThat(LlmNetworkProxyConfiguration.parseProxySettings("https://proxy.internal", null, null))
                .extracting("scheme", "host", "port", "proxyType")
                .containsExactly("https", "proxy.internal", 443, reactor.netty.transport.ProxyProvider.Proxy.HTTP);
    }

    @Test
    void supportsAuthenticatedSocks5Proxy() {
        assertThat(LlmNetworkProxyConfiguration.parseProxySettings(
                "socks5://127.0.0.1:1081", "proxy-user", "proxy-password"))
                .extracting("scheme", "host", "port", "proxyType", "username", "password")
                .containsExactly(
                        "socks5",
                        "127.0.0.1",
                        1081,
                        reactor.netty.transport.ProxyProvider.Proxy.SOCKS5,
                        "proxy-user",
                        "proxy-password");
    }

    @Test
    void rejectsUnsupportedOrEmbeddedCredentials() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LlmNetworkProxyConfiguration.parseProxySettings(
                        "ftp://127.0.0.1:21", null, null))
                .withMessageContaining("Expected http://, https://, or socks5://");
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LlmNetworkProxyConfiguration.parseProxySettings(
                        "http://user:secret@127.0.0.1:7890", null, null))
                .withMessageContaining("Expected http://, https://, or socks5://");
    }

    @Test
    void rejectsPasswordWithoutUsername() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> LlmNetworkProxyConfiguration.parseProxySettings(
                        "http://127.0.0.1:7890", null, "proxy-password"))
                .withMessageContaining("LLM_NETWORK_PROXY_USERNAME is required");
    }
}
