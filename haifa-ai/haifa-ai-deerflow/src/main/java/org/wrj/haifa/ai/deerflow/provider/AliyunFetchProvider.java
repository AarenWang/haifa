package org.wrj.haifa.ai.deerflow.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

import java.util.Map;

/**
 * Aliyun ReadPageBasic (IQS) web fetch provider.
 */
@Component
public class AliyunFetchProvider implements WebFetchProvider {

    private static final Logger log = LoggerFactory.getLogger(AliyunFetchProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired(required = false)
    private DeerFlowProperties properties;

    private final RestClient restClient;

    public AliyunFetchProvider() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(30000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public WebFetchProviderType type() {
        return WebFetchProviderType.ALIYUN;
    }

    @Override
    public String fetch(String url) {
        String apiKey = getApiKey();
        log.info("Fetching URL content using Aliyun IQS ReadPageBasic: {}", url);

        try {
            Map<String, Object> requestBody = Map.of(
                    "url", url,
                    "maxAge", 0
            );

            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri("https://cloud-iqs.aliyuncs.com/readpage/basic")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody);

            if (apiKey != null && !apiKey.isBlank()) {
                requestSpec.header("X-API-Key", apiKey);
                requestSpec.header("Authorization", "Bearer " + apiKey);
            }

            String responseBody = requestSpec.retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Empty response received from Aliyun IQS ReadPageBasic");
            }

            JsonNode root = MAPPER.readTree(responseBody);
            String errorCode = root.path("errorCode").asText("");
            String errorMessage = root.path("errorMessage").asText("");
            if (!errorCode.isEmpty() || !errorMessage.isEmpty()) {
                log.warn("Aliyun IQS ReadPageBasic returned error: [{}] {}", errorCode, errorMessage);
                throw new RuntimeException("Aliyun IQS ReadPageBasic error: " + errorMessage);
            }

            JsonNode dataNode = root.get("data");
            if (dataNode != null) {
                String markdown = dataNode.path("markdown").asText("");
                if (!markdown.isBlank()) {
                    return markdown;
                }
                String text = dataNode.path("text").asText("");
                if (!text.isBlank()) {
                    return text;
                }
                String html = dataNode.path("html").asText("");
                if (!html.isBlank()) {
                    return html;
                }
            }

            throw new RuntimeException("No parsed content (markdown/text/html) found in the response");

        } catch (Exception e) {
            log.warn("Aliyun IQS fetch failed for URL: {}, falling back to stub. Error: {}", url, e.getMessage());
            return getStubResponse(url);
        }
    }

    private String getApiKey() {
        String apiKey = null;
        if (properties != null && properties.getTools() != null && properties.getTools().getWebFetch() != null) {
            apiKey = properties.getTools().getWebFetch().getApiKey();
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("ALIYUN_IQS_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        return apiKey;
    }

    private String getStubResponse(String url) {
        return """
                Fetched content from: %s
                (Provider: Aliyun ReadPageBasic — stub response fallback)
                This is placeholder content. The Aliyun ReadPageBasic provider fell back
                to stub response because the HTTP request failed or was offline.
                """.formatted(url).strip();
    }
}
