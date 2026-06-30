package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Registry of {@link WebSearchProvider} implementations.
 *
 * <p>Allows lookup by {@link WebSearchProviderType} and resolves the configured
 * default provider.</p>
 */
@Component
public class WebSearchProviderRegistry {

    private final Map<WebSearchProviderType, WebSearchProvider> providers = new ConcurrentHashMap<>();

    public WebSearchProviderRegistry(List<WebSearchProvider> providerList) {
        for (WebSearchProvider provider : providerList) {
            providers.put(provider.type(), provider);
        }
    }

    /**
     * Get a provider by type.
     *
     * @throws IllegalArgumentException if no provider is registered for the type
     */
    public WebSearchProvider get(WebSearchProviderType type) {
        WebSearchProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "No WebSearchProvider registered for type: " + type.id() +
                    ". Registered: " + providers.keySet().stream().map(WebSearchProviderType::id).toList());
        }
        return provider;
    }

    /**
     * Resolve the provider for the given type id string.
     */
    public WebSearchProvider resolve(String id) {
        WebSearchProviderType type = WebSearchProviderType.fromId(id);
        return get(type);
    }

    public WebSearchProvider defaultProvider() {
        return get(WebSearchProviderType.defaultType());
    }

    public List<WebSearchProvider> allProviders() {
        return List.copyOf(providers.values());
    }

    public boolean hasProvider(WebSearchProviderType type) {
        return providers.containsKey(type);
    }
}
