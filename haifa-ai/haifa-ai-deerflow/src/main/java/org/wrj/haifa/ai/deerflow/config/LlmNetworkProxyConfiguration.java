package org.wrj.haifa.ai.deerflow.config;

import java.net.URI;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import reactor.netty.transport.ProxyProvider;

/** Configures the network proxy used by Spring AI's blocking and streaming model clients. */
@Configuration(proxyBeanMethods = false)
public class LlmNetworkProxyConfiguration {

    private static final Logger log = LoggerFactory.getLogger(LlmNetworkProxyConfiguration.class);

    @Bean
    @Primary
    RestClient.Builder llmProxyRestClientBuilder(DeerFlowProperties properties) {
        ProxySettings proxy = proxySettings(properties);
        if (proxy == null) {
            return RestClient.builder();
        }
        log.info("Configuring LLM {} proxy for blocking requests. proxy={}:{}",
                proxy.scheme(), proxy.host(), proxy.port());
        return configureRestClientBuilder(RestClient.builder(), proxy);
    }

    static RestClient.Builder configureRestClientBuilder(RestClient.Builder builder, ProxySettings proxy) {
        reactor.netty.http.client.HttpClient httpClient = proxyHttpClient(proxy);
        ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
        return builder.requestFactory(requestFactory);
    }

    @Bean
    WebClientCustomizer llmProxyWebClientCustomizer(DeerFlowProperties properties) {
        ProxySettings proxy = proxySettings(properties);
        if (proxy == null) {
            return builder -> { };
        }
        log.info("Configuring LLM {} proxy for streaming requests. proxy={}:{}",
                proxy.scheme(), proxy.host(), proxy.port());
        reactor.netty.http.client.HttpClient httpClient = proxyHttpClient(proxy);
        ReactorClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        return builder -> builder.clientConnector(connector);
    }

    static ProxySettings proxySettings(DeerFlowProperties properties) {
        return parseProxySettings(
                properties.getNetworkProxyUrl(),
                properties.getNetworkProxyUsername(),
                properties.getNetworkProxyPassword());
    }

    private static reactor.netty.http.client.HttpClient proxyHttpClient(ProxySettings proxy) {
        return reactor.netty.http.client.HttpClient.create().proxy(spec -> {
            ProxyProvider.Builder builder = spec.type(proxy.proxyType())
                    .host(proxy.host())
                    .port(proxy.port());
            if (StringUtils.hasText(proxy.username())) {
                builder.username(proxy.username())
                        .password(ignored -> proxy.password() == null ? "" : proxy.password());
            }
        });
    }

    static ProxySettings parseProxySettings(String configuredProxy, String username, String password) {
        if (!StringUtils.hasText(configuredProxy)) {
            if (StringUtils.hasText(username) || StringUtils.hasText(password)) {
                throw new IllegalArgumentException(
                        "LLM proxy username or password is configured, but LLM_NETWORK_PROXY_URL is empty");
            }
            return null;
        }
        if (!StringUtils.hasText(username) && StringUtils.hasText(password)) {
            throw new IllegalArgumentException(
                    "LLM_NETWORK_PROXY_USERNAME is required when LLM_NETWORK_PROXY_PASSWORD is configured");
        }

        URI proxyUri;
        try {
            proxyUri = URI.create(configuredProxy.trim());
        } catch (IllegalArgumentException ex) {
            throw invalidProxy(ex);
        }

        String scheme = proxyUri.getScheme();
        String normalizedScheme = scheme == null ? "" : scheme.toLowerCase(Locale.ROOT);
        if (!(normalizedScheme.equals("http") || normalizedScheme.equals("https")
                || normalizedScheme.equals("socks5"))) {
            throw invalidProxy(null);
        }
        if (!StringUtils.hasText(proxyUri.getHost()) || proxyUri.getUserInfo() != null
                || proxyUri.getQuery() != null || proxyUri.getFragment() != null
                || (StringUtils.hasText(proxyUri.getPath()) && !"/".equals(proxyUri.getPath()))) {
            throw invalidProxy(null);
        }

        int port = proxyUri.getPort();
        if (port < 0) {
            port = switch (normalizedScheme) {
                case "https" -> 443;
                case "socks5" -> 1080;
                default -> 80;
            };
        }
        if (port == 0 || port > 65_535) {
            throw invalidProxy(null);
        }
        ProxyProvider.Proxy proxyType = normalizedScheme.equals("socks5")
                ? ProxyProvider.Proxy.SOCKS5
                : ProxyProvider.Proxy.HTTP;
        return new ProxySettings(
                normalizedScheme,
                proxyUri.getHost(),
                port,
                proxyType,
                StringUtils.hasText(username) ? username : null,
                StringUtils.hasText(username) ? (password == null ? "" : password) : null);
    }

    private static IllegalArgumentException invalidProxy(Exception cause) {
        String message = "Invalid LLM network proxy URL. Expected http://, https://, or socks5:// followed by host and port";
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }

    record ProxySettings(
            String scheme,
            String host,
            int port,
            ProxyProvider.Proxy proxyType,
            String username,
            String password) { }

}
