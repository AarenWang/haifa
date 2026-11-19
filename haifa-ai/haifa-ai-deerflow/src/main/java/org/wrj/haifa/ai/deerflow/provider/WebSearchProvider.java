package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;

/**
 * Provider implementation for web search.
 */
public interface WebSearchProvider {

    /**
     * Returns the provider type this implementation handles.
     */
    WebSearchProviderType type();

    /**
     * Execute a web search.
     *
     * @param query the search query
     * @param maxResults maximum number of results to return
     * @return search result text
     */
    String search(String query, int maxResults);

    /**
     * Returns metadata for this provider.
     */
    default ProviderMetadata metadata() {
        return type().toMetadata();
    }
}
