package org.wrj.haifa.ai.deerflow.provider;

import org.springframework.stereotype.Component;

/**
 * DuckDuckGo web search provider.
 *
 * <p>Default search provider. No API key required. Currently returns a stub
 * response; full HTTP implementation can be added later without changing the
 * tool contract.</p>
 */
@Component
public class DuckDuckGoSearchProvider implements WebSearchProvider {

    @Override
    public WebSearchProviderType type() {
        return WebSearchProviderType.DUCKDUCKGO;
    }

    @Override
    public String search(String query, int maxResults) {
        // Stub: return a formatted mock result
        // TODO: integrate with DuckDuckGo API or DDGS library
        return """
                Search results for: %s
                (Provider: DuckDuckGo — stub response)
                1. [Example Result] https://example.com/result-1
                   Summary: Sample result from DuckDuckGo stub provider.
                """.formatted(query).strip();
    }
}
