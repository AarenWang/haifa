package org.wrj.haifa.ai.spring.toolcalling.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties that capture model preferences and access details for
 * the external geography knowledge service.
 */
@ConfigurationProperties(prefix = "haifa.ai.tool-calling")
public class ToolCallingProperties {

    /**
     * Preferred model identifier that the chat client should target.
     */
    private String model = "gpt-4o-mini";

    /**
     * Optional prefix that will be prepended to answers returned by the REST controller.
     */
    private String responsePrefix = "According to our knowledge base, ";

    private final GeoProperties geo = new GeoProperties();

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getResponsePrefix() {
        return responsePrefix;
    }

    public void setResponsePrefix(String responsePrefix) {
        this.responsePrefix = responsePrefix;
    }

    public GeoProperties getGeo() {
        return geo;
    }

    public static class GeoProperties {

        /**
         * Base URL used for the geographic knowledge API.
         */
        private String baseUrl = "https://example.com/api"\;

        /**
         * Relative path invoked on the knowledge API when performing lookups.
         */
        private String path = "/geo";

        /**
         * Optional API key that will be sent as {@code X-API-Key} header.
         */
        private String apiKey;

        /**
         * Request timeout applied to knowledge API calls.
         */
        private Duration timeout = Duration.ofSeconds(5);

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public Duration getTimeout() {
            return timeout;
        }

        public void setTimeout(Duration timeout) {
            this.timeout = timeout;
        }
    }
}
