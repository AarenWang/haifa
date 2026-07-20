package org.wrj.haifa.ai.utilitymcp.config;

import java.net.URI;
import java.util.Locale;
import org.springframework.util.StringUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

/** Builds the per-provider proxy configuration used for outbound utility requests. */
public final class UtilityNetworkProxyConfiguration {

    private UtilityNetworkProxyConfiguration() { }

    public static ProxySettings proxySettings(
            String providerId,
            UtilityMcpProperties.Proxy configuredProxy,
            UtilityMcpProperties.Provider provider) {
        if (!provider.isProxyEnabled()) return null;
        ProxySettings settings = parseProxySettings(
                configuredProxy.getUrl(), configuredProxy.getUsername(), configuredProxy.getPassword());
        if (settings == null) {
            throw new IllegalArgumentException(
                    "Proxy is enabled for provider " + providerId + ", but UTILITY_MCP_PROXY_URL is empty");
        }
        return settings;
    }

    public static HttpClient configure(HttpClient client, ProxySettings proxy) {
        if (proxy == null) return client;
        return client.proxy(spec -> {
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
                        "Utility MCP proxy username or password is configured, but UTILITY_MCP_PROXY_URL is empty");
            }
            return null;
        }
        if (!StringUtils.hasText(username) && StringUtils.hasText(password)) {
            throw new IllegalArgumentException(
                    "UTILITY_MCP_PROXY_USERNAME is required when UTILITY_MCP_PROXY_PASSWORD is configured");
        }

        URI proxyUri;
        try {
            proxyUri = URI.create(configuredProxy.trim());
        }
        catch (IllegalArgumentException ex) {
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
        if (port == 0 || port > 65_535) throw invalidProxy(null);
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
        String message = "Invalid Utility MCP proxy URL. Expected http://, https://, or socks5:// followed by host and port";
        return cause == null ? new IllegalArgumentException(message) : new IllegalArgumentException(message, cause);
    }

    public record ProxySettings(
            String scheme,
            String host,
            int port,
            ProxyProvider.Proxy proxyType,
            String username,
            String password) { }
}
