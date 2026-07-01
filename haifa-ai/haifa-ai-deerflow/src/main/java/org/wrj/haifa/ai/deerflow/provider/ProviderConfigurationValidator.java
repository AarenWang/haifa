package org.wrj.haifa.ai.deerflow.provider;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.wrj.haifa.ai.deerflow.config.DeerFlowProperties;

/**
 * Validates configured tool providers at application startup.
 *
 * <p>Ensures that:
 * <ol>
 *   <li>The configured provider ID is a known enum value.</li>
 *   <li>The configured provider has a registered implementation bean.</li>
 *   <li>If the provider requires an API key, one is configured.</li>
 * </ol>
 *
 * <p>Failures are reported as {@link IllegalStateException} with clear messages
 * naming the missing configuration property or environment variable.</p>
 */
@Component
public class ProviderConfigurationValidator {

    private final DeerFlowProperties properties;
    private final WebSearchProviderRegistry searchRegistry;
    private final WebFetchProviderRegistry fetchRegistry;

    public ProviderConfigurationValidator(DeerFlowProperties properties,
            WebSearchProviderRegistry searchRegistry,
            WebFetchProviderRegistry fetchRegistry) {
        this.properties = properties;
        this.searchRegistry = searchRegistry;
        this.fetchRegistry = fetchRegistry;
    }

    @PostConstruct
    public void validate() {
        validateWebSearchProvider();
        validateWebFetchProvider();
    }

    private void validateWebSearchProvider() {
        String providerId = properties.getWebSearchProvider();
        // 1. ID must be a known enum value
        WebSearchProviderType type;
        try {
            type = WebSearchProviderType.fromId(providerId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }

        // 2. Must have a registered implementation bean
        if (!searchRegistry.hasProvider(type)) {
            List<String> registered = searchRegistry.allProviders().stream()
                    .map(p -> p.type().id())
                    .toList();
            throw new IllegalStateException(
                    "Configured web_search provider '" + providerId + "' is not registered. "
                    + "Registered providers: " + registered + ". "
                    + "To use this provider, add its implementation bean to the Spring context."
            );
        }

        // 3. If API key is required, it must be configured
        if (type.requiresApiKey()) {
            String apiKey = properties.getTools().getWebSearch().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "Configured web_search provider '" + providerId + "' requires an API key, "
                        + "but none is configured. "
                        + "Please set: haifa.ai.deerflow.tools.web-search.api-key="
                        + apiKeyEnvVarHint(type) + ""
                );
            }
        }
    }

    private void validateWebFetchProvider() {
        String providerId = properties.getWebFetchProvider();
        // 1. ID must be a known enum value
        WebFetchProviderType type;
        try {
            type = WebFetchProviderType.fromId(providerId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }

        // 2. Must have a registered implementation bean
        if (!fetchRegistry.hasProvider(type)) {
            List<String> registered = fetchRegistry.allProviders().stream()
                    .map(p -> p.type().id())
                    .toList();
            throw new IllegalStateException(
                    "Configured web_fetch provider '" + providerId + "' is not registered. "
                    + "Registered providers: " + registered + ". "
                    + "To use this provider, add its implementation bean to the Spring context."
            );
        }

        // 3. If API key is required, it must be configured
        if (type.requiresApiKey()) {
            String apiKey = properties.getTools().getWebFetch().getApiKey();
            if (apiKey == null || apiKey.isBlank()) {
                throw new IllegalStateException(
                        "Configured web_fetch provider '" + providerId + "' requires an API key, "
                        + "but none is configured. "
                        + "Please set: haifa.ai.deerflow.tools.web-fetch.api-key="
                        + apiKeyEnvVarHint(type) + ""
                );
            }
        }
    }

    private static String apiKeyEnvVarHint(WebSearchProviderType type) {
        return switch (type) {
            case TAVILY -> " (env: TAVILY_API_KEY)";
            case BRAVE -> " (env: BRAVE_SEARCH_API_KEY)";
            case EXA -> " (env: EXA_API_KEY)";
            case FIRECRAWL -> " (env: FIRECRAWL_API_KEY)";
            case INFOQUEST -> " (env: INFOQUEST_API_KEY)";
            case GROUNDROUTE -> " (env: GROUNDROUTE_API_KEY)";
            case SERPER -> " (env: SERPER_API_KEY)";
            case FASTCRW -> " (env: CRW_API_KEY)";
            case ALIYUN -> " (env: ALIYUN_API_KEY or DASHSCOPE_API_KEY)";
            default -> "";
        };
    }

    private static String apiKeyEnvVarHint(WebFetchProviderType type) {
        return switch (type) {
            case JINA -> " (env: JINA_API_KEY)";
            case EXA -> " (env: EXA_API_KEY)";
            case FIRECRAWL -> " (env: FIRECRAWL_API_KEY)";
            case INFOQUEST -> " (env: INFOQUEST_API_KEY)";
            case GROUNDROUTE -> " (env: GROUNDROUTE_API_KEY)";
            case BROWSERLESS -> " (env: BROWSERLESS_TOKEN)";
            case FASTCRW -> " (env: CRW_API_KEY)";
            case ALIYUN -> " (env: ALIYUN_API_KEY or DASHSCOPE_API_KEY)";
            default -> "";
        };
    }
}
