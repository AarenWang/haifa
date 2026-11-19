package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;

/**
 * Metadata describing a web search or web fetch provider.
 */
public record ProviderMetadata(
        String id,
        String displayName,
        boolean requiresApiKey,
        boolean defaultEnabled,
        boolean supportsSearch,
        boolean supportsFetch,
        String notes
) {
}
