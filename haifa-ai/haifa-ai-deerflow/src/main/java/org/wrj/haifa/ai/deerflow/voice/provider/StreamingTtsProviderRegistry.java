package org.wrj.haifa.ai.deerflow.voice.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class StreamingTtsProviderRegistry {
    private final Map<String, StreamingTtsProvider> providers = new ConcurrentHashMap<>();

    public StreamingTtsProviderRegistry(List<StreamingTtsProvider> providerList) {
        if (providerList != null) {
            for (StreamingTtsProvider p : providerList) {
                providers.put(p.id().toLowerCase(), p);
            }
        }
    }

    public void register(StreamingTtsProvider provider) {
        providers.put(provider.id().toLowerCase(), provider);
    }

    public Optional<StreamingTtsProvider> get(String id) {
        if (id == null) return Optional.empty();
        return Optional.ofNullable(providers.get(id.toLowerCase()));
    }
}
