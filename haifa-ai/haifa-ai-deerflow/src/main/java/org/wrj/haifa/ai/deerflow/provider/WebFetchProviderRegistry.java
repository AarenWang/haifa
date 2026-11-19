package org.wrj.haifa.ai.deerflow.provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Registry of {@link WebFetchProvider} implementations.
 *
 * <p>Allows lookup by {@link WebFetchProviderType} and resolves the configured
 * default provider.</p>
 */
@Component
public class WebFetchProviderRegistry {

    private final Map<WebFetchProviderType, WebFetchProvider> providers = new ConcurrentHashMap<>();

    public WebFetchProviderRegistry(List<WebFetchProvider> providerList) {
        for (WebFetchProvider provider : providerList) {
            providers.put(provider.type(), provider);
        }
    }

    /**
     * Get a provider by type.
     *
     * @throws IllegalArgumentException if no provider is registered for the type
     */
    public WebFetchProvider get(WebFetchProviderType type) {
        WebFetchProvider provider = providers.get(type);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "No WebFetchProvider registered for type: " + type.id() +
                    ". Registered: " + providers.keySet().stream().map(WebFetchProviderType::id).toList());
        }
        return provider;
    }

    /**
     * Resolve the provider for the given type id string.
     */
    public WebFetchProvider resolve(String id) {
        WebFetchProviderType type = WebFetchProviderType.fromId(id);
        return get(type);
    }

    public WebFetchProvider defaultProvider() {
        return get(WebFetchProviderType.defaultType());
    }

    public List<WebFetchProvider> allProviders() {
        return List.copyOf(providers.values());
    }

    public boolean hasProvider(WebFetchProviderType type) {
        return providers.containsKey(type);
    }
}
