package org.wrj.haifa.ai.deerflow.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

/**
 * Jina AI Reader web fetch provider.
 *
 * <p>Default fetch provider. Anonymous usage supported (with rate limits).
 * Integrates with Jina AI Reader API (https://r.jina.ai/http://URL).</p>
 */
@Component
public class JinaAiFetchProvider implements WebFetchProvider {

    private static final Logger log = LoggerFactory.getLogger(JinaAiFetchProvider.class);

    @Autowired(required = false)
    private DeerFlowProperties properties;

    private final RestClient restClient;

    public JinaAiFetchProvider() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(30000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public WebFetchProviderType type() {
        return WebFetchProviderType.JINA;
    }

    @Override
    public String fetch(String url) {
        String apiKey = null;
        if (properties != null && properties.getTools() != null && properties.getTools().getWebFetch() != null) {
            apiKey = properties.getTools().getWebFetch().getApiKey();
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("JINA_API_KEY");
        }

        log.info("Fetching URL content using Jina AI Reader: {}", url);

        try {
            RestClient.RequestHeadersSpec<?> request = restClient.get()
                    .uri("https://r.jina.ai/{url}", url);

            if (apiKey != null && !apiKey.isBlank()) {
                request.header("Authorization", "Bearer " + apiKey);
            }

            String content = request.retrieve()
                    .body(String.class);

            if (content == null || content.isBlank()) {
                throw new RuntimeException("Empty response received from Jina AI Reader");
            }

            return content;
        } catch (Exception e) {
            log.warn("Jina AI fetch failed for URL: {}, falling back to stub. Error: {}", url, e.getMessage());
            return getStubResponse(url);
        }
    }

    private String getStubResponse(String url) {
        return """
                Fetched content from: %s
                (Provider: Jina AI Reader — stub response fallback)
                This is placeholder content. The Jina AI Reader provider fell back
                to stub response because the HTTP request failed or was offline.
                """.formatted(url).strip();
    }
}
