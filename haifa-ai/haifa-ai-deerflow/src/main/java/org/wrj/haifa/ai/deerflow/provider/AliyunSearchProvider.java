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
 * Aliyun UnifiedSearch (IQS) web search provider.
 */
@Component
public class AliyunSearchProvider implements WebSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(AliyunSearchProvider.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired(required = false)
    private DeerFlowProperties properties;

    private final RestClient restClient;

    public AliyunSearchProvider() {
        org.springframework.http.client.SimpleClientHttpRequestFactory requestFactory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(10000);
        requestFactory.setReadTimeout(30000);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    @Override
    public WebSearchProviderType type() {
        return WebSearchProviderType.ALIYUN;
    }

    @Override
    public String search(String query, int maxResults) {
        String apiKey = getApiKey();
        log.info("Performing Aliyun IQS UnifiedSearch for query: '{}', maxResults: {}", query, maxResults);

        try {
            Map<String, Object> requestBody = Map.of(
                    "query", query,
                    "engineType", "LiteAdvanced",
                    "contents", Map.of(
                            "mainText", true,
                            "markdownText", false,
                            "summary", false,
                            "rerankScore", true
                    ),
                    "advancedParams", Map.of(
                            "numResults", maxResults
                    )
            );

            RestClient.RequestBodySpec requestSpec = restClient.post()
                    .uri("https://cloud-iqs.aliyuncs.com/search/unified")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody);

            if (apiKey != null && !apiKey.isBlank()) {
                requestSpec.header("Authorization", "Bearer " + apiKey);
            }

            String responseBody = requestSpec.retrieve()
                    .body(String.class);

            if (responseBody == null || responseBody.isBlank()) {
                throw new RuntimeException("Empty response received from Aliyun IQS UnifiedSearch");
            }

            JsonNode root = MAPPER.readTree(responseBody);
            String errorCode = root.path("errorCode").asText("");
            String errorMessage = root.path("errorMessage").asText("");
            if (!errorCode.isEmpty() || !errorMessage.isEmpty()) {
                log.warn("Aliyun IQS UnifiedSearch returned error: [{}] {}", errorCode, errorMessage);
                throw new RuntimeException("Aliyun IQS UnifiedSearch error: " + errorMessage);
            }

            JsonNode pageItems = root.get("pageItems");
            StringBuilder sb = new StringBuilder();
            sb.append("Search results for: ").append(query).append("\n");
            sb.append("(Provider: Aliyun)\n");

            int count = 0;
            if (pageItems != null && pageItems.isArray()) {
                for (JsonNode item : pageItems) {
                    count++;
                    String title = item.path("title").asText("").trim();
                    String link = item.path("link").asText("").trim();
                    String snippet = item.path("snippet").asText("").trim();

                    sb.append(count).append(". [").append(title).append("] ").append(link).append("\n");
                    if (!snippet.isEmpty()) {
                        sb.append("   Summary: ").append(snippet).append("\n");
                    }
                }
            }

            if (count == 0) {
                log.warn("Failed to parse any valid result entries. Falling back to stub.");
                return getStubResponse(query);
            }

            return sb.toString().strip();

        } catch (Exception e) {
            log.warn("Aliyun IQS UnifiedSearch failed for query: '{}', falling back to stub. Error: {}", query, e.getMessage());
            return getStubResponse(query);
        }
    }

    private String getApiKey() {
        String apiKey = null;
        if (properties != null && properties.getTools() != null && properties.getTools().getWebSearch() != null) {
            apiKey = properties.getTools().getWebSearch().getApiKey();
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("ALIYUN_IQS_API_KEY");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = System.getenv("DASHSCOPE_API_KEY");
        }
        return apiKey;
    }

    private String getStubResponse(String query) {
        return """
                Search results for: %s
                (Provider: Aliyun — stub response fallback)
                1. [Example Result] https://example.com/result-1
                   Summary: Sample result from Aliyun search stub provider.
                """.formatted(query).strip();
    }
}
