package org.wrj.haifa.ai.utilitymcp.config;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "haifa.ai.utility-mcp")
public class UtilityMcpProperties {

    private final Security security = new Security();
    private final Proxy proxy = new Proxy();
    private final Providers providers = new Providers();
    private int targetResultBytes = 65_536;
    private int maxResultBytes = 262_144;

    public Security getSecurity() { return security; }
    public Proxy getProxy() { return proxy; }
    public Providers getProviders() { return providers; }
    public int getTargetResultBytes() { return targetResultBytes; }
    public void setTargetResultBytes(int targetResultBytes) { this.targetResultBytes = targetResultBytes; }
    public int getMaxResultBytes() { return maxResultBytes; }
    public void setMaxResultBytes(int maxResultBytes) { this.maxResultBytes = maxResultBytes; }

    public static class Security {
        private List<String> allowedOrigins = new ArrayList<>();
        private boolean allowMissingOrigin = true;
        private int requestsPerMinute = 120;
        private String audience = "";

        public List<String> getAllowedOrigins() { return allowedOrigins; }
        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins == null ? new ArrayList<>() : new ArrayList<>(allowedOrigins);
        }
        public boolean isAllowMissingOrigin() { return allowMissingOrigin; }
        public void setAllowMissingOrigin(boolean allowMissingOrigin) { this.allowMissingOrigin = allowMissingOrigin; }
        public int getRequestsPerMinute() { return requestsPerMinute; }
        public void setRequestsPerMinute(int requestsPerMinute) { this.requestsPerMinute = requestsPerMinute; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
    }

    public static class Providers {
        private final Provider openMeteo = new Provider("https://api.open-meteo.com");
        private final Provider openMeteoGeocoding = new Provider("https://geocoding-api.open-meteo.com");
        private final Provider openMeteoAirQuality = new Provider("https://air-quality-api.open-meteo.com");
        private final Provider frankfurter = new Provider("https://api.frankfurter.app");
        private final Provider nagerDate = new Provider("https://date.nager.at");
        private final Provider wikimedia = new Provider("https://api.wikimedia.org");

        public Provider getOpenMeteo() { return openMeteo; }
        public Provider getOpenMeteoGeocoding() { return openMeteoGeocoding; }
        public Provider getOpenMeteoAirQuality() { return openMeteoAirQuality; }
        public Provider getFrankfurter() { return frankfurter; }
        public Provider getNagerDate() { return nagerDate; }
        public Provider getWikimedia() { return wikimedia; }
    }

    public static class Proxy {
        private String url = "";
        private String username = "";
        private String password = "";

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class Provider {
        private URI baseUrl;
        private Duration connectTimeout = Duration.ofSeconds(2);
        private Duration responseTimeout = Duration.ofSeconds(8);
        private int maxResponseBytes = 1_048_576;
        private int maxConcurrent = 16;
        private Duration cacheTtl = Duration.ofMinutes(10);
        private boolean allowHttpForTests;
        private boolean proxyEnabled;

        public Provider(String baseUrl) { this.baseUrl = URI.create(baseUrl); }
        public URI getBaseUrl() { return baseUrl; }
        public void setBaseUrl(URI baseUrl) { this.baseUrl = baseUrl; }
        public Duration getConnectTimeout() { return connectTimeout; }
        public void setConnectTimeout(Duration connectTimeout) { this.connectTimeout = connectTimeout; }
        public Duration getResponseTimeout() { return responseTimeout; }
        public void setResponseTimeout(Duration responseTimeout) { this.responseTimeout = responseTimeout; }
        public int getMaxResponseBytes() { return maxResponseBytes; }
        public void setMaxResponseBytes(int maxResponseBytes) { this.maxResponseBytes = maxResponseBytes; }
        public int getMaxConcurrent() { return maxConcurrent; }
        public void setMaxConcurrent(int maxConcurrent) { this.maxConcurrent = maxConcurrent; }
        public Duration getCacheTtl() { return cacheTtl; }
        public void setCacheTtl(Duration cacheTtl) { this.cacheTtl = cacheTtl; }
        public boolean isAllowHttpForTests() { return allowHttpForTests; }
        public void setAllowHttpForTests(boolean allowHttpForTests) { this.allowHttpForTests = allowHttpForTests; }
        public boolean isProxyEnabled() { return proxyEnabled; }
        public void setProxyEnabled(boolean proxyEnabled) { this.proxyEnabled = proxyEnabled; }
    }
}
