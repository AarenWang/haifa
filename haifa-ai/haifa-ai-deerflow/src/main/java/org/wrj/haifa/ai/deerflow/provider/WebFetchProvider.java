package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;

/**
 * Provider implementation for web fetch (page content extraction).
 */
public interface WebFetchProvider {

    /**
     * Returns the provider type this implementation handles.
     */
    WebFetchProviderType type();

    /**
     * Fetch and extract the content of a web page.
     *
     * @param url the URL to fetch
     * @return extracted page content (e.g., markdown)
     */
    String fetch(String url);

    /**
     * Returns metadata for this provider.
     */
    default ProviderMetadata metadata() {
        return type().toMetadata();
    }
}
