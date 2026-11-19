package org.wrj.haifa.ai.deerflow.voice.provider;

import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StreamingAsrProviderRegistry {
    private final Map<String, StreamingAsrProvider> providers = new ConcurrentHashMap<>();

    public StreamingAsrProviderRegistry(List<StreamingAsrProvider> providerList) {
        if (providerList != null) {
            for (StreamingAsrProvider p : providerList) {
                providers.put(p.id().toLowerCase(), p);
            }
        }
    }

    public void register(StreamingAsrProvider provider) {
        providers.put(provider.id().toLowerCase(), provider);
    }

    public Optional<StreamingAsrProvider> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(providers.get(id.toLowerCase()));
    }
}
